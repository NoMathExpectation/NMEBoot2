package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

class RegexNotMatchException(val regex: Regex) : CommandParseException("Failed to match regex: $regex") {
    override val showToUser = false
}

class RegexMatchCommandNode<S>(
    val regex: Regex,
    override var next: CommandNode<S> = commandNodeTodo(),
) : SingleNextCommandNode<S> {
    val storeArguments: MutableMap<Int, String> = mutableMapOf()
    var matchHelp: String = "<regex>"

    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        regex.find(context.reader.string)?.let {
            storeArguments.forEach { (pos, name) ->
                context[name] = it.groupValues[pos]
            }

            return next.execute(context)
        }

        return ExecuteResult(
            context.source,
            0,
            1,
            exceptions = listOf(RegexNotMatchException(regex))
        )
    }

    override suspend fun completion(context: CommandContext<S>) = next.completion(context)?.let {
        HelpOption.Options(
            listOf(matchHelp to it)
        )
    }

    override suspend fun help(context: CommandContext<S>) = next.help(context)?.let {
        HelpOption.Options(
            listOf(matchHelp to it)
        )
    }
}

fun <S> RegexMatchCommandNode<S>.withMatchHelp(help: String) = apply { matchHelp = help }

fun <S> RegexMatchCommandNode<S>.storeGroupValue(position: Int, name: String) = apply {
    storeArguments[position] = name
}

fun <S> InsertableCommandNode<S>.onMatchRegex(regex: Regex) =
    RegexMatchCommandNode<S>(regex).also { insert(it) }