package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

class NotImplementedCommandNode<S> : CommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        return ExecuteResult(
            context.source,
            0,
            1,
            parseExceptions = listOf(NotImplementedError("此指令尚未实现。"))
        )
    }
}

fun <S> commandNodeTodo() = NotImplementedCommandNode<S>()

fun <S> InsertableCommandNode<S>.todo() = commandNodeTodo<S>().also { insert(it) }