package NoMathExpectation.NMEBoot.command.parser.node

fun <S> InsertableCommandNode<S>.onEndOfReader() =
    on { reader.peekChar() == null }

fun <S> InsertableCommandNode<S>.onEndOfArguments() =
    on { reader.peekWord() == null }