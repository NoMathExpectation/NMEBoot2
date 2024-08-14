package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data

import kotlinx.serialization.Serializable

@Serializable
internal data class RequestedParameter(
    val collection_name: String,
    val per_page: Int,
    val q: String
)
