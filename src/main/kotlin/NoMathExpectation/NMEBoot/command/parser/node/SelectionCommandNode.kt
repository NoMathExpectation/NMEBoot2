package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

class SelectionCommandNode<S>(
    val options: MutableList<CommandNode<S>> = mutableListOf()
) : InsertableCommandNode<S> {
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