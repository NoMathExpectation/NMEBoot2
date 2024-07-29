package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

typealias ConditionClause<S> = suspend CommandContext<S>.(S) -> Boolean

class ConditionFailedException(message: String) : RuntimeException(message)

class ConditionCommandNode<S>(
    override var next: CommandNode<S> = commandNodeTodo(),
    val condition: ConditionClause<S>,
    var failException: Throwable? = null,
) : SingleNextCommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        if (condition(context, context.source)) {
            return next.execute(context)
        }
        val exception = failException
        return ExecuteResult(
            context.source,
            0,
            1,
            parseExceptions = if (exception != null) listOf(exception) else listOf()
        )
    }

    fun withFailMessage(message: String) = this.apply { failException = ConditionFailedException(message) }

    fun withFailException(exception: Throwable) = this.apply { failException = exception }
}

fun <S> InsertableCommandNode<S>.on(clause: ConditionClause<S>) =
    ConditionCommandNode(condition = clause).also { insert(it) }