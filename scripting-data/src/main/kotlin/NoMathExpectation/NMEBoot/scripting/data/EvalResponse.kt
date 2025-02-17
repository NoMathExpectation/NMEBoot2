package NoMathExpectation.NMEBoot.scripting.data

import kotlinx.serialization.Serializable

@Serializable
data class EvalResponse(
    val output: String,
    val returns: String?,
    val exception: String?
)
