package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult
import NoMathExpectation.NMEBoot.command.parser.ParserDsl

@ParserDsl
fun interface CommandNode<S> {
    suspend fun execute(context: CommandContext<S>): ExecuteResult<S>
}