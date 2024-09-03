package NoMathExpectation.NMEBoot.bot

import NoMathExpectation.NMEBoot.command.impl.commandConfig
import NoMathExpectation.NMEBoot.command.impl.executeCommand
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.message.onebot.OneBotFileCache
import NoMathExpectation.NMEBoot.message.removeReferencePrefix
import NoMathExpectation.NMEBoot.message.toReadableString
import NoMathExpectation.NMEBoot.util.nickOrName
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.component.onebot.v11.core.event.notice.OneBotGroupUploadEvent
import love.forte.simbot.event.*

private val logger = KotlinLogging.logger { }
internal val messageLogger = KotlinLogging.logger("Messages")

internal suspend fun handleEvent(event: Event) {
    logEvent(event)

    if (event is OneBotGroupUploadEvent) {
        OneBotFileCache.record(event)
    }

    tryHandleCommand(event)
}

internal suspend fun logEvent(event: Event) {
    if (event !is MessageEvent) {
        logger.debug { event }
        return
    }

    val msgString = event.messageContent.messages.toReadableString()

    when (event) {
        is ChatChannelMessageEvent -> {
            val source = event.source()
            val content = event.content()
            val author = event.author()
            messageLogger.info {
                "Bot.${event.bot.id}.rx [${source.name}(${source.id})][${content.name}(${content.id})] ${author.nickOrName}(${event.authorId}) -> $msgString"
            }
        }

        is ChatGroupMessageEvent -> {
            val content = event.content()
            val author = event.author()
            messageLogger.info {
                "Bot.${event.bot.id}.rx [${content.name}(${content.id})] ${author.name}(${event.authorId}) -> $msgString"
            }
        }

        is ChatRoomMessageEvent -> {
            val content = event.content()
            messageLogger.info {
                "Bot.${event.bot.id}.rx [${content.name}(${content.id})] <unknown>(${event.authorId}) -> $msgString"
            }
        }

        is MemberMessageEvent -> {
            val source = event.source()
            val content = event.content()
            messageLogger.info {
                "Bot.${event.bot.id}.rx [${source.name}(${source.id})][private] ${content.name}(${content.id}) -> $msgString"
            }
        }

        is ContactMessageEvent -> {
            val content = event.content()
            messageLogger.info {
                "Bot.${event.bot.id}.rx [contact] ${content.name}(${content.id}) -> $msgString"
            }
        }

        else -> messageLogger.info {
            "Bot.${event.bot.id}.rx [unknown] <unknown>(${event.authorId}) -> $msgString"
        }
    }
}

internal suspend fun tryHandleCommand(event: Event) {
    if (event !is MessageEvent) {
        return
    }

    val source = CommandSource.get(event) ?: return
    val text = event.messageContent.messages.removeReferencePrefix().toReadableString()


    val prefix = commandConfig.get().commandPrefix
    if (!text.startsWith(prefix)) {
        return
    }

    source.executeCommand(text.removePrefix(prefix)) {
        originalMessage = event.messageContent
    }
}