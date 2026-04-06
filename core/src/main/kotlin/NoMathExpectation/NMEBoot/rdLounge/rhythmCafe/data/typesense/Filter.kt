package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.typesense

import kotlinx.serialization.Serializable

@Serializable
internal data class Filter(
    val counts: List<FilterItem>,
    val field_name: String,
    val stats: FilterStat
)
