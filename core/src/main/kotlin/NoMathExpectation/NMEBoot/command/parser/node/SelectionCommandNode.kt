package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

class SelectionCommandNode<S>(
    val options: MutableList<CommandNode<S>> = mutableListOf()
) : InsertableCommandNode<S> {
    var blockOptions: Boolean = true
    var help: String = ""

    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        val exceptions = mutableListOf<CommandException>()
        options.forEach {
            val copied = context.copy()
            val result = it.execute(copied)
            if (result.accepted > 0) {
                return result
            }

            exceptions += result.exceptions
        }
        return ExecuteResult(
            context.source,
            0,
            1,
            exceptions = exceptions,
        )
    }

    override suspend fun completion(context: CommandContext<S>) =
        buildList {
            options.mapNotNull {
                it.completion(context.copy())
            }.forEach {
                when (it) {
                    is HelpOption.Options -> addAll(it.options)
                    is HelpOption.Help -> add(null to it)
                }
            }
        }.takeIf {
            it.isNotEmpty()
        }?.let {
            HelpOption.Options(it)
        }

    override suspend fun help(context: CommandContext<S>) = if (blockOptions) {
        HelpOption.Help(
            help,
            true,
        )
    } else buildList {
        options.mapNotNull {
            it.help(context.copy())
        }.forEach {
            when (it) {
                is HelpOption.Options -> addAll(it.options)
                is HelpOption.Help -> add(null to it)
            }
        }
    }.takeIf {
        it.isNotEmpty()
    }?.let {
        HelpOption.Options(it)
    }


    override fun insert(commandNode: CommandNode<S>) {
        options += commandNode
    }

    operator fun plusAssign(node: CommandNode<S>) = insert(node)
}

inline fun <S> InsertableCommandNode<S>.select(init: SelectionCommandNode<S>.() -> Unit): SelectionCommandNode<S> {
    val node = SelectionCommandNode<S>()
    node.init()
    insert(node)
    return node
}