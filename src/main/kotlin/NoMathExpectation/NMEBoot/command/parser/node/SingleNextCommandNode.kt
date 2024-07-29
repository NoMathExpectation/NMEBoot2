package NoMathExpectation.NMEBoot.command.parser.node

interface SingleNextCommandNode<S> : InsertableCommandNode<S> {
    var next: CommandNode<S>

    override fun insert(commandNode: CommandNode<S>) {
        next = commandNode
    }
}