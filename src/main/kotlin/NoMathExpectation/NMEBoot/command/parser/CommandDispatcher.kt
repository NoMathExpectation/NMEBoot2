package NoMathExpectation.NMEBoot.command.parser

import NoMathExpectation.NMEBoot.command.parser.node.CommandNode
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.commandNodeTodo

class CommandDispatcher<S>(
    var root: CommandNode<S> = commandNodeTodo()
) {
    suspend fun dispatch(source: S, reader: StringReader): ExecuteResult<S> {
        val context = CommandContext(source, reader)
        return root.execute(context)
    }

    suspend fun dispatch(source: S, commandString: String) = dispatch(source, StringReader(commandString))
}

inline fun <S> CommandDispatcher(block: LiteralSelectionCommandNode<S>.() -> Unit) =
    CommandDispatcher(LiteralSelectionCommandNode<S>().apply(block))

inline fun <S, N : CommandNode<S>> CommandDispatcher(root: N, block: N.() -> Unit) =
    CommandDispatcher(root.apply(block))