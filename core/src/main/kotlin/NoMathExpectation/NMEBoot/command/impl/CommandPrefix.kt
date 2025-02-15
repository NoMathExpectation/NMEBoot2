package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult
import NoMathExpectation.NMEBoot.command.parser.node.*

class PrefixNotMatchException(val prefix: String) : CommandParseException("Prefix not match: $prefix") {
    override val showToUser = false
}

class PrefixCheckNode(
    val prefix: String,
    override var next: CommandNode<AnyExecuteContext> = commandNodeTodo(),
) : SingleNextCommandNode<AnyExecuteContext> {
    override suspend fun execute(context: CommandContext<AnyExecuteContext>): ExecuteResult<AnyExecuteContext> {
        if (!context.source.requiresCommandPrefix || context.reader.peekString(prefix.length) == prefix) {
            if (context.reader.peekString(prefix.length) == prefix) {
                context.reader.next += prefix.length
            }
            return next.execute(context)
        }
        return ExecuteResult(
            context.source,
            0,
            1,
            exceptions = listOf(PrefixNotMatchException(prefix))
        )
    }

    override suspend fun completion(context: CommandContext<AnyExecuteContext>): HelpOption? {
        if (context.reader.peekString(prefix.length) == prefix) {
            context.reader.next += prefix.length
        }
        return next.completion(context)
    }

    override suspend fun help(context: CommandContext<AnyExecuteContext>) = if (context.source.requiresCommandPrefix) {
        when (val nextResult = next.help(context)) {
            null -> null
            is HelpOption.Help -> HelpOption.Options(
                listOf(prefix to nextResult)
            )

            is HelpOption.Options -> HelpOption.Options(
                nextResult.options.map {
                    "${prefix}${it.first ?: ""}" to it.second
                }
            )
        }
    } else {
        next.help(context)
    }
}

internal fun InsertableCommandNode<AnyExecuteContext>.onCommandPrefix(prefix: String) =
    PrefixCheckNode(prefix).also { insert(it) }