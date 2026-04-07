package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.searchV2

import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.LevelStatus
import kotlinx.serialization.Serializable

@Serializable
internal data class CafeSearchResult(
    val estimatedTotalHits: Int,
    val hits: List<LevelStatus>,
    val limit: Int,
    val offset: Int,
    val processingTimeMs: Long,
    val query: String,
)
