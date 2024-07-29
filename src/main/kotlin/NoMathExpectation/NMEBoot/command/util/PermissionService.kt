package NoMathExpectation.NMEBoot.command.util

import NoMathExpectation.NMEBoot.util.storageOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

object PermissionService {
    @Serializable
    class PermissionNode(
        val children: MutableMap<String, PermissionNode> = mutableMapOf(),
        val allow: MutableMap<String, Boolean> = mutableMapOf()
    )

    private val rootStore = storageOf(
        "permission", PermissionNode(
            allow = mutableMapOf("console" to true)
        )
    )

    private val logger = KotlinLogging.logger { }

    private suspend fun getNodes(path: String): List<PermissionNode> {
        val root = rootStore.get()
        val nodes = mutableListOf(root)
        var current = root
        for (name in path.split(".")) {
            if (name == "*") {
                break
            }

            val next = current.children[name] ?: break
            nodes += next
            current = next
        }
        nodes.reverse()
        return nodes
    }

    private suspend fun makeNodes(path: String): List<PermissionNode> {
        val nodes = mutableListOf<PermissionNode>()
        rootStore.referenceUpdate { root ->
            nodes += root
            var current = root
            for (name in path.split(".")) {
                if (name == "*") {
                    break
                }

                val next = current.children.getOrPut(name) { PermissionNode() }
                nodes += next
                current = next
            }
        }
        nodes.reverse()
        return nodes
    }

    suspend fun hasPermission(path: String, vararg ids: String): Boolean {
        val nodes = getNodes(path)
        ids.reversed().forEach { id ->
            nodes.forEach { node ->
                val status = node.allow[id]
                if (status != null) {
                    return status
                }
            }
        }
        return false
    }

    suspend fun setPermission(path: String, id: String, allow: Boolean? = null) {
        rootStore.referenceUpdate {
            val node = makeNodes(path).first()
            if (allow != null) {
                node.allow[id] = allow
            } else {
                node.allow.remove(id)
            }
        }
    }
}