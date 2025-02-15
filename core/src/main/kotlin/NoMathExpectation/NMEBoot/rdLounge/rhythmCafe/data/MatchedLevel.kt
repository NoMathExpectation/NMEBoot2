package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data

import kotlinx.serialization.Serializable

@Serializable
internal data class MatchedLevel(
    val document: Level,
    val highlights: List<MatchedField>,
    val text_match: Long
)
