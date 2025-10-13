package NoMathExpectation.NMEBoot.bot

import NoMathExpectation.NMEBoot.message.onebot.apiExt.OnebotExtApi
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.application.Application
import love.forte.simbot.component.kook.kookBots
import love.forte.simbot.component.onebot.v11.core.bot.register
import love.forte.simbot.component.onebot.v11.core.oneBot11Bots

private val logger = KotlinLogging.logger { }

@Serializable
sealed interface BotConfig {
    suspend fun Application.register()
}

@Serializable
@SerialName("onebot")
data class OneBotConfig(
    val apiHost: String,
    val eventHost: String,
    val id: String,
    val token: String,
    val extApiType: OnebotExtApi.Type = OnebotExtApi.Type.LAGRANGE,
) : BotConfig {
    override suspend fun Application.register() {
        oneBot11Bots {
            logger.info { "添加 OneBot : ${this@OneBotConfig}" }

            val bot = register {
                botUniqueId = id
                apiServerHost = Url(apiHost)
                eventServerHost = Url(eventHost)
                apiAccessToken = token
                eventAccessToken = token
            }

            OnebotExtApi.registerBotType(bot, extApiType)

            bot.start()
        }
    }
}

@Serializable
@SerialName("kook")
data class KookBotConfig(
    val id: String,
    val secret: String,
) : BotConfig {
    override suspend fun Application.register() {
        kookBots {
            logger.info { "添加 KookBot : ${this@KookBotConfig}" }

            val bot = registerWs(id, secret)

            bot.start()
        }
    }
}