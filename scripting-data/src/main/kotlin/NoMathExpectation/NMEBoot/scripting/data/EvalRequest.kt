package NoMathExpectation.NMEBoot.scripting.data

data class EvalRequest(
    val source: String,
    val input: String,
    val quoted: String,
)
