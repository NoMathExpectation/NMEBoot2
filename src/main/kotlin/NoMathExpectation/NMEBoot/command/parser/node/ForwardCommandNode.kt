package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

class ForwardCommandNode<S>(
    override var next: CommandNode<S> = commandNodeTodo()
) : SingleNextCommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        return next.execute(context)
    }
}

fun <S> InsertableCommandNode<S>.forward(next: CommandNode<S>) = ForwardCommandNode(next).also { insert(it) }