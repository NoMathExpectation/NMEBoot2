package NoMathExpectation.NMEBoot.command.parser

data class ExecuteResult<out S>(
    val source: S,
    val accepted: Int,
    val forks: Int,
    val exceptions: List<CommandException> = listOf(),
) {
    init {
        require(accepted >= 0) { "Accepted count cannot be negative." }
        require(forks >= 0) { "Fork count cannot be negative." }
        require(accepted <= forks) { "Accepted count cannot be greater than fork count." }
    }
}