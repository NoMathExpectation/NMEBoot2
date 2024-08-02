package NoMathExpectation.NMEBoot.bot

import NoMathExpectation.NMEBoot.command.impl.commandConfig
import NoMathExpectation.NMEBoot.command.impl.executeCommand
import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.message.toReadableString
import NoMathExpectation.NMEBoot.util.nickOrName
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.event.*

private val logger = KotlinLogging.logger { }
internal val messageLogger = KotlinLogging.logger("Messages")

internal suspend fun handleEvent(event: Event) {
    logEvent(event)
    tryHandleCommand(event)
}

internal fun logEvent(event: Event) {
    if (event !is MessageEvent) {
        logger.debug { event }
        return
    }

    when (event) {
        is ChatChannelMessageEvent -> messageLogger.info {
            "Bot.${event.bot.id}.rx [${event.source.name}(${event.source.id})][${event.content.name}(${event.content.id})] ${event.author.nickOrName}(${event.authorId}) -> ${event.messageContent.messages.toReadableString()}"
        }

        is ChatGroupMessageEvent -> messageLogger.info {
            "Bot.${event.bot.id}.rx [${event.content.name}(${event.content.id})] ${event.author.name}(${event.authorId}) -> ${event.messageContent.messages.toReadableString()}"
        }

        is ChatRoomMessageEvent -> messageLogger.info {
            "Bot.${event.bot.id}.rx [${event.content.name}(${event.content.id})] <unknown>(${event.authorId}) -> ${event.messageContent.messages.toReadableString()}"
        }

        is MemberMessageEvent -> messageLogger.info {
            "Bot.${event.bot.id}.rx [${event.source.name}(${event.source.id})][private] ${event.content.name}(${event.content.id}) -> ${event.messageContent.messages.toReadableString()}"
        }

        is ContactMessageEvent -> messageLogger.info {
            "Bot.${event.bot.id}.rx [contact] ${event.content.name}(${event.content.id}) -> ${event.messageContent.messages.toReadableString()}"
        }

        else -> messageLogger.info {
            "Bot.${event.bot.id}.rx [unknown] <unknown>(${event.authorId}) -> ${event.messageContent.messages.toReadableString()}"
        }
    }
}

internal suspend fun tryHandleCommand(event: Event) {
    if (event !is MessageEvent) {
        return
    }

    val source = CommandSource.get(event) ?: return
    val text = event.messageContent.messages.toReadableString()


    val prefix = commandConfig.get().commandPrefix
    if (!text.startsWith(prefix)) {
        return
    }

    source.executeCommand(text.removePrefix(prefix))
}