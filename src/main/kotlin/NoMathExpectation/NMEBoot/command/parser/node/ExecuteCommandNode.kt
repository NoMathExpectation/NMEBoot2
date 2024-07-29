package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

typealias ExecuteClause<S> = suspend CommandContext<S>.(S) -> Unit

class ExecuteCommandNode<S>(
    private val executes: ExecuteClause<S>,
) : CommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        val exception = kotlin.runCatching {
            executes(context, context.source)
        }.exceptionOrNull()
        return ExecuteResult(
            context.source,
            1,
            1,
            executeExceptions = if (exception != null) listOf(exception) else listOf(),
        )
    }
}

fun <S> InsertableCommandNode<S>.executes(executes: ExecuteClause<S>) =
    ExecuteCommandNode(executes).also { insert(it) }