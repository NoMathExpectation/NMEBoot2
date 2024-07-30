package NoMathExpectation.NMEBoot.command.util

import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.on
import NoMathExpectation.NMEBoot.command.source.ConsoleCommandSource.uidToPermissionId
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

    const val anyonePermissionId = "*"

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
        val root = rootStore.get()
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
        nodes.reverse()
        return nodes
    }

    suspend fun hasPermission(path: String, vararg ids: String): Boolean {
        val nodes = getNodes(path)
        listOf(*ids, anyonePermissionId).forEach { id ->
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

interface PermissionAware {
    val permissionIds: List<String>

    suspend fun hasPermission(permission: String): Boolean {
        return PermissionService.hasPermission(permission, *permissionIds.toTypedArray())
    }

    suspend fun setPermission(permission: String, value: Boolean?) {
        PermissionService.setPermission(permission, uidToPermissionId, value)
    }
}

suspend fun <S : PermissionAware> InsertableCommandNode<S>.requirePermission(
    permission: String,
    defaultPermission: Boolean = false
) =
    on {
        PermissionService.hasPermission(permission, *it.permissionIds.toTypedArray())
    }.also {
        PermissionService.setPermission(permission, PermissionService.anyonePermissionId, defaultPermission)
    }