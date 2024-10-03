package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult
import NoMathExpectation.NMEBoot.command.parser.argument.ArgumentCollector

class ArgumentCollectException(throwable: Throwable) :
    CommandParseException(throwable.message ?: "无效的参数", throwable)

class ArgumentCollectCommandNode<S, T>(
    val name: String,
    override var next: CommandNode<S> = commandNodeTodo(),
    var collector: ArgumentCollector<S, T>
) : SingleNextCommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        val collected = kotlin.runCatching {
            collector.collect(context)
        }.getOrElse {
            return ExecuteResult(
                context.source,
                0,
                1,
                exceptions = listOf(ArgumentCollectException(it))
            )
        }

        context[name] = collected
        return next.execute(context)
    }
}

fun <S, T> InsertableCommandNode<S>.collect(name: String, collector: ArgumentCollector<S, T>) =
    ArgumentCollectCommandNode(name, collector = collector).also { insert(it) }