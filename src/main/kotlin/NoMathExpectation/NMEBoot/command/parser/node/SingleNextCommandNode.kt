package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext

interface SingleNextCommandNode<S> : InsertableCommandNode<S> {
    var next: CommandNode<S>

    override fun insert(commandNode: CommandNode<S>) {
        next = commandNode
    }

    val nextImplemented get() = next !is NotImplementedCommandNode

    override suspend fun completion(context: CommandContext<S>) = next.completion(context)

    override suspend fun help(context: CommandContext<S>) = next.help(context)
}