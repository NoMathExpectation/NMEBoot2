package NoMathExpectation.NMEBoot.bot

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface BotConfig {
}

@Serializable
@SerialName("onebot")
data class OneBotConfig(
    val apiHost: String,
    val eventHost: String,
    val id: String,
    val token: String,
) : BotConfig