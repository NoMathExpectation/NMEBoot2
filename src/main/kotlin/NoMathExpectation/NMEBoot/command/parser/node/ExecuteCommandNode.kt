package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandExecuteException
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

typealias ExecuteClause<S> = suspend CommandContext<S>.(S) -> Unit

class ExecuteCommandNode<S>(
    private val executes: ExecuteClause<S>,
    val help: String = "",
    override var next: CommandNode<S> = commandNodeTodo(),
) : SingleNextCommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        val exception = kotlin.runCatching {
            executes(context, context.source)
        }.exceptionOrNull()
        return if (nextImplemented) {
            exception?.let {
                ExecuteResult(
                    context.source,
                    0,
                    1,
                    exceptions = listOf(CommandParseException(it)),
                )
            } ?: next.execute(context)
        } else ExecuteResult(
            context.source,
            1,
            1,
            exceptions = if (exception != null) listOf(CommandExecuteException(exception)) else listOf(),
        )
    }

    override suspend fun completion(context: CommandContext<S>) = if (nextImplemented) {
        next.completion(context)
    } else {
        HelpOption.Help(help)
    }

    override suspend fun help(context: CommandContext<S>) = if (nextImplemented) {
        next.help(context)
    } else {
        HelpOption.Help(help)
    }
}

fun <S> InsertableCommandNode<S>.executes(help: String = "", executes: ExecuteClause<S>) =
    ExecuteCommandNode(executes, help).also { insert(it) }