package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.typesense

import kotlinx.serialization.Serializable

@Serializable
internal data class FilterItem(
    val count: Int,
    val highlighted: String,
    val value: String
)
