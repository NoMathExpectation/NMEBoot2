package NoMathExpectation.NMEBoot.bot

import NoMathExpectation.NMEBoot.util.storageOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import love.forte.simbot.component.kook.useKook
import love.forte.simbot.component.onebot.v11.core.useOneBot11
import love.forte.simbot.core.application.SimpleApplication
import love.forte.simbot.core.application.launchSimpleApplication
import love.forte.simbot.event.Event
import love.forte.simbot.event.process

@Serializable
private data class AppConfig(
    val bots: List<BotConfig> = listOf()
)

private val logger = KotlinLogging.logger { }

private val config = storageOf("config/app.json", AppConfig())

var simbotApplication: SimpleApplication? = null
    private set
private val appMutex = Mutex()

internal suspend fun startSimbot() {
    logger.info { "启动Simbot中......" }

    appMutex.withLock {
        simbotApplication = launchSimpleApplication {
            useOneBot11()
            useKook()
        }

        simbotApplication?.let { app ->
            config.get().bots.forEach { bot ->
                with(bot) {
                    app.register()
                }
            }
        }

        simbotApplication?.eventDispatcher?.process<Event> {
            handleEvent(it)
        }
    }

    logger.info { "启动完成" }
}

internal suspend fun stopSimbot() {
    logger.info { "结束Simbot中......" }

    appMutex.withLock {
        simbotApplication?.cancel()
        simbotApplication?.join()
        simbotApplication = null
    }

    logger.info { "Simbot已结束" }
}