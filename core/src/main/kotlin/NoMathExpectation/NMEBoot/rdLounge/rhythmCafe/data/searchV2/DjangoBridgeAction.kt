package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.searchV2

import kotlinx.serialization.Serializable

@Serializable
data class DjangoBridgeAction<out T>(val props: T)
