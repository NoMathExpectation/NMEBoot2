package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.searchV2

import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.LevelStatus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CafeIndexProps(
    @SerialName("daily_blend_level")
    val dailyBlendLevel: LevelStatus? = null
)
