package NoMathExpectation.NMEBoot.bot

import NoMathExpectation.NMEBoot.command.impl.command.common.MCChat
import NoMathExpectation.NMEBoot.command.impl.executeCommand
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.message.onebot.OneBotFileCache
import NoMathExpectation.NMEBoot.message.removeReferencePrefix
import NoMathExpectation.NMEBoot.message.standardize
import NoMathExpectation.NMEBoot.message.toReadableStringWithCICode
import NoMathExpectation.NMEBoot.message.toSerialized
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

    tryNotifyMCServers(event)
    tryHandleCommand(event)
}

internal suspend fun logEvent(event: Event) {
    if (event !is MessageEvent) {
        logger.debug { event }
        return
    }

    val actor = (event as? ActorEvent)?.content()
    val msgString = event.messageContent.messages.standardize().toSerialized(actor)

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
    val actor = (event as? ActorEvent)?.content()
    val text = event.messageContent.messages.standardize().removeReferencePrefix().toSerialized(actor)

    source.executeCommand(text) {
        originalMessage = event.messageContent
    }
}

internal suspend fun tryNotifyMCServers(event: Event) {
    if (event !is ChatRoomMessageEvent) {
        return
    }

    val source = CommandSource.get(event) ?: return
    var content = event.messageContent
        .messages
        .removeReferencePrefix()
        .standardize()
        .toReadableStringWithCICode(source.globalSubject)

    val subjectId = source.subjectPermissionId ?: return
    if (MCChat.isIgnoredSender(
            subjectId,
            source.id
        ) && !(content.startsWith("@broadcast") || content.startsWith("@bc") || content.startsWith("@广播"))
    ) {
        return
    }

    content = if (content.startsWith("@broadcast")) {
        content.removePrefix("@broadcast")
    } else if (content.startsWith("@bc")) {
        content.removePrefix("@bc")
    } else if (content.startsWith("@广播")) {
        content.removePrefix("@广播")
    } else {
        content
    }.trimStart()

    if (event.authorId == event.bot.id /* && MCChat.checkEchoAndRemove(content) */) {
        return
    }

    val name = source.executor?.nickOrName ?: "unknown"
    MCChat.sendMessage(subjectId, name, content)
}