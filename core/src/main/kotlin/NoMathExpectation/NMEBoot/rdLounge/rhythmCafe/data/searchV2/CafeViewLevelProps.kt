package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.searchV2

import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.LevelStatus
import kotlinx.serialization.Serializable

@Serializable
internal data class CafeViewLevelProps(
    val rdlevel: LevelStatus,
)