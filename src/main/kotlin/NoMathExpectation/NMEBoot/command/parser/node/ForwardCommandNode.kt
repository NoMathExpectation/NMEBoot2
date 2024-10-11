package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

class ForwardCommandNode<S>(
    override var next: CommandNode<S> = commandNodeTodo(),
    var help: String? = null,
) : SingleNextCommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        return next.execute(context)
    }

    override suspend fun help(context: CommandContext<S>) = help?.let {
        HelpOption.Help(
            it,
            true,
        )
    } ?: next.help(context)
}

fun <S> ForwardCommandNode<S>.withHelp(help: String) = apply { this.help = help }

fun <S> InsertableCommandNode<S>.forward(next: CommandNode<S>, help: String? = null) =
    ForwardCommandNode(next, help).also { insert(it) }