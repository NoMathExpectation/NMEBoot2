package NoMathExpectation.NMEBoot.command.parser.node

interface InsertableCommandNode<S> : CommandNode<S> {
    fun insert(commandNode: CommandNode<S>)
}