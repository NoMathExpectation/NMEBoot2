package NoMathExpectation.NMEBoot.command.parser

sealed class CommandException : RuntimeException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable)
    constructor(message: String, throwable: Throwable) : super(message, throwable)

    open val showToUser = false
}

open class CommandParseException : CommandException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable)
    constructor(message: String, throwable: Throwable) : super(message, throwable)

    override val showToUser = true
}

open class CommandExecuteException : CommandException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(throwable: Throwable) : super(throwable)
    constructor(message: String, throwable: Throwable) : super(message, throwable)

    override val showToUser = false
}