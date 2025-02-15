package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult
import NoMathExpectation.NMEBoot.command.parser.node.CommandNode
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.SingleNextCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.commandNodeTodo
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
        "data/permission.json", PermissionNode(
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
        val root = rootStore.get()
        val nodes = mutableListOf(root)
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

    suspend fun copyPermission(fromPath: String, toPath: String, deep: Boolean = false) {
        rootStore.referenceUpdate {
            val from = getNodes(fromPath).first()
            val to = makeNodes(toPath).first()

            to.allow.clear()
            to.allow.putAll(from.allow)
            if (deep) {
                to.children.clear()
                to.children.putAll(from.children)
            }
        }
    }

    suspend fun movePermission(fromPath: String, toPath: String, deep: Boolean = false) {
        rootStore.referenceUpdate {
            val from = getNodes(fromPath).first()
            val to = makeNodes(toPath).first()

            to.allow.clear()
            to.allow.putAll(from.allow)
            if (deep) {
                to.children.clear()
                to.children.putAll(from.children)
            }

            from.allow.clear()
            if (deep) {
                from.children.clear()
            }
        }
    }
}

interface PermissionAware {
    suspend fun hasPermission(permission: String): Boolean

    suspend fun setPermission(permission: String, value: Boolean?)
}

interface PermissionServiceAware : PermissionAware {
    val primaryPermissionId: String

    val permissionIds: List<String>
        get() = listOf(primaryPermissionId)

    override suspend fun hasPermission(permission: String): Boolean {
        return PermissionService.hasPermission(permission, *permissionIds.toTypedArray())
    }

    override suspend fun setPermission(permission: String, value: Boolean?) {
        PermissionService.setPermission(permission, primaryPermissionId, value)
    }
}

class CommandPermissionDeniedException(val source: PermissionAware, val permission: String) :
    CommandParseException("${(source as? PermissionServiceAware)?.permissionIds} 没有 $permission 权限") {
    override val showToUser = false
}

class PermissionCheckCommandNode<S : PermissionAware> private constructor(
    val permission: String,
    val defaultPermissions: Map<String, Boolean?> = mapOf(),
    override var next: CommandNode<S> = commandNodeTodo(),
) : SingleNextCommandNode<S> {
    private suspend fun init() {
        defaultPermissions.forEach { (id, value) ->
            PermissionService.setPermission(permission, id, value)
        }
    }

    override suspend fun execute(context: CommandContext<S>) = if (context.source.hasPermission(permission)) {
        next.execute(context)
    } else {
        ExecuteResult(
            context.source,
            0,
            1,
            exceptions = listOf(CommandPermissionDeniedException(context.source, permission))
        )
    }

    override suspend fun completion(context: CommandContext<S>) =
        context.source
            .hasPermission(permission)
            .takeIf { it }
            ?.let { next.completion(context) }

    override suspend fun help(context: CommandContext<S>) =
        context.source
            .hasPermission(permission)
            .takeIf { it }
            ?.let { next.help(context) }

    companion object {
        suspend operator fun <S : PermissionAware> invoke(
            permission: String,
            defaultPermissions: Map<String, Boolean?> = mapOf()
        ) =
            PermissionCheckCommandNode<S>(permission, defaultPermissions).also {
                it.init()
            }
    }
}

suspend fun <S : PermissionAware> InsertableCommandNode<S>.requiresPermission(
    permission: String,
    defaultPermissions: Map<String, Boolean?> = mapOf(),
) =
    PermissionCheckCommandNode<S>(permission, defaultPermissions).also {
        insert(it)
    }

suspend fun <S : PermissionAware> InsertableCommandNode<S>.requiresPermission(
    permission: String,
    vararg defaultPermissions: Pair<String, Boolean?>,
) =
    PermissionCheckCommandNode<S>(permission, defaultPermissions.toMap()).also {
        insert(it)
    }