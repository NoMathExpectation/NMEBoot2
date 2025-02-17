package NoMathExpectation.NMEBoot.scripting.data

import kotlinx.serialization.Serializable

@Serializable
data class EvalRequest(
    val source: String,
    val input: String? = null,
    val quoted: String? = null,
    val timeout: Long = 30 * 1000,
)
