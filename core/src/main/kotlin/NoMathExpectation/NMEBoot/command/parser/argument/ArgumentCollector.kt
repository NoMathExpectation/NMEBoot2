package NoMathExpectation.NMEBoot.command.parser.argument

import NoMathExpectation.NMEBoot.command.parser.CommandContext

fun interface ArgumentCollector<in S, out T> {
    suspend fun collect(context: CommandContext<S>): T

    fun buildHelp(name: String) = "<$name>"
}