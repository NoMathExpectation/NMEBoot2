@file:OptIn(ExperimentalTime::class)

package NoMathExpectation.NMEBoot.bot

import NoMathExpectation.NMEBoot.command.impl.command.common.MCChat
import NoMathExpectation.NMEBoot.command.impl.command.common.pokeEventForHistory
import NoMathExpectation.NMEBoot.command.impl.command.rd.handleSamurai
import NoMathExpectation.NMEBoot.command.impl.executeCommand
import NoMathExpectation.NMEBoot.command.impl.source.*
import NoMathExpectation.NMEBoot.database.message.MessageHistory
import NoMathExpectation.NMEBoot.message.*
import NoMathExpectation.NMEBoot.message.event.CommandSourcePostSendEvent
import NoMathExpectation.NMEBoot.message.event.CommandSourcePreSendEvent
import NoMathExpectation.NMEBoot.message.onebot.OneBotFileCache
import NoMathExpectation.NMEBoot.util.nickOrName
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.component.onebot.v11.core.event.meta.OneBotHeartbeatEvent
import love.forte.simbot.component.onebot.v11.core.event.notice.OneBotBotSelfPokeEvent
import love.forte.simbot.component.onebot.v11.core.event.notice.OneBotGroupUploadEvent
import love.forte.simbot.event.*
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger { }
internal val eventLogger = KotlinLogging.logger("Events")
internal val messageLogger = KotlinLogging.logger("Messages")

internal suspend fun handleEvent(event: Event) {
    val source = CommandSource.get(event)
    logEvent(event, source)

    if (event is CommandSourcePreSendEvent<*>) {
        handleSamurai(event)
        return
    }

    if (event is OneBotGroupUploadEvent) {
        OneBotFileCache.record(event)
    }

    tryNotifyMCServers(event)

    if (event is OneBotBotSelfPokeEvent) {
        pokeEventForHistory(event)
        return
    }

    source?.let {
        tryHandleCommand(event, it)
    }
}

internal suspend fun logEvent(event: Event, source: CommandSource<*>?) {
    when (event) {
        is OneBotHeartbeatEvent, is InternalMessagePreSendEvent -> return
        is InternalMessagePostSendEvent -> {
            logPostSendMessage(event)
            MessageHistory.logPostSendMessage(event)
        }

        is MessageEvent -> {
            logMessageEvent(event)
            MessageHistory.logMessage(event, source)
        }

        else -> eventLogger.info { event }
    }
}

private suspend fun logMessageEvent(event: MessageEvent) {
    if (event.bot.isMe(event.authorId)) {
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

private suspend fun logPostSendMessage(event: InternalMessagePostSendEvent) {
    if (event !is CommandSourcePostSendEvent<*>) {
        return
    }

    val source = event.content
    val msgString = event.message.message.toSerialized(event.target())
    when (source) {
        is GuildMemberCommandSource -> {
            val globalSubject = source.globalSubject
            val subject = source.subject
            messageLogger.info {
                "Bot.${source.bot.id}.tx [${globalSubject.name}(${globalSubject.id})][${subject.name}(${subject.id})] <- $msgString"
            }
        }

        is ChatGroupMemberCommandSource -> {
            val subject = source.subject
            messageLogger.info {
                "Bot.${source.bot.id}.tx [${subject.name}(${subject.id})] <- $msgString"
            }
        }

        is MemberPrivateCommandSource -> {
            val globalSubject = source.globalSubject
            val subject = source.subject
            messageLogger.info {
                "Bot.${source.bot.id}.tx [${globalSubject.name}(${globalSubject.id})][private] ${subject.name}(${subject.id}) <- $msgString"
            }
        }

        is ContactCommandSource -> {
            val subject = source.subject
            messageLogger.info {
                "Bot.${source.bot.id}.tx [contact] ${subject.name}(${subject.id}) <- $msgString"
            }
        }

        is ConsoleCommandSource -> {
            // skip, since we already log console messages when sending
        }

        else -> messageLogger.info {
            val globalSubject = source.globalSubject
            val subject = source.subject
            "Bot.${source.bot?.id}.tx [unknown(${globalSubject?.id})] <unknown>(${subject?.id}) <- $msgString"
        }
    }
}

internal suspend fun tryHandleCommand(event: Event, source: CommandSource<*>) {
    if (event !is MessageEvent) {
        return
    }

    val actor = (event as? ActorEvent)?.content()
    val text = event.messageContent
        .messages
        .standardize()
        .removeReferencePrefix()
        .toSerialized(actor) {
            formatLineFeeds = false
        }

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

    content = (if (content.startsWith("@broadcast")) {
        content.removePrefix("@broadcast")
    } else if (content.startsWith("@bc")) {
        content.removePrefix("@bc")
    } else if (content.startsWith("@广播")) {
        content.removePrefix("@广播")
    } else {
        content
    }).trimStart()

    if (event.authorId == event.bot.id /* && MCChat.checkEchoAndRemove(content) */) {
        return
    }

    val name = source.executor?.nickOrName ?: "unknown"
    MCChat.sendMessage(subjectId, name, content)
}