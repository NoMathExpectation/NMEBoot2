package NoMathExpectation.NMEBoot.scripting.data

data class EvalResponse(
    val output: String,
    val returns: String,
    val exception: String?
)
