package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

typealias RawConditionClause<S> = suspend CommandContext<S>.(S) -> Unit
typealias ConditionClause<S> = suspend CommandContext<S>.(S) -> Boolean

class ConditionFailedException : CommandParseException {
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable.message ?: "检定失败", throwable)
}

class ConditionCommandNode<S>(
    override var next: CommandNode<S> = commandNodeTodo(),
    var reportOnFail: Boolean = false,
    val condition: RawConditionClause<S>,
) : SingleNextCommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        val exception = kotlin.runCatching {
            condition(context, context.source)
            return next.execute(context)
        }.getOrElse { ConditionFailedException(it) }
        return ExecuteResult(
            context.source,
            0,
            1,
            exceptions = if (reportOnFail) listOf(exception) else listOf(),
        )
    }

    var helpCondition: ConditionClause<S>? = null

    override suspend fun completion(context: CommandContext<S>) = when {
        reportOnFail -> next.completion(context)
        helpCondition?.invoke(context, context.source) == true -> next.completion(context)
        else -> kotlin.runCatching {
            condition(context, context.source)
            next.completion(context)
        }.getOrNull()
    }

    override suspend fun help(context: CommandContext<S>) = when {
        reportOnFail -> next.help(context)
        helpCondition?.invoke(context, context.source) == true -> next.help(context)
        else -> kotlin.runCatching {
            condition(context, context.source)
            next.help(context)
        }.getOrNull()
    }
}

fun <S> InsertableCommandNode<S>.onNotFail(clause: RawConditionClause<S>) =
    ConditionCommandNode(condition = clause).also { insert(it) }

fun <S> InsertableCommandNode<S>.on(failMessage: String = "", clause: ConditionClause<S>) =
    ConditionCommandNode {
        if (!clause(it)) throw ConditionFailedException(failMessage)
    }.also { insert(it) }

fun <S> ConditionCommandNode<S>.reportOnFail(boolean: Boolean = true) = apply { reportOnFail = boolean }

fun <S> ConditionCommandNode<S>.overrideHelpCondition(clause: ConditionClause<S>) = apply {
    helpCondition = clause
}