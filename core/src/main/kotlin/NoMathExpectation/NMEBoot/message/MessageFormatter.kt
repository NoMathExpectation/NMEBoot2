package NoMathExpectation.NMEBoot.message

import NoMathExpectation.NMEBoot.message.format.MessageElementFormatter
import NoMathExpectation.NMEBoot.message.format.SerializedMessage
import NoMathExpectation.NMEBoot.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegmentElement
import love.forte.simbot.component.onebot.v11.message.segment.OneBotReply
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.*

private val logger = KotlinLogging.logger { }

object MessageFormatter {
    val elementFormatters by lazy {
        koin.koin.getAll<MessageElementFormatter<Message.Element>>()
    }

    private fun String.escapeIdentifiers(escapeLineFeeds: Boolean = true) = buildString {
        this@escapeIdentifiers.forEach {
            when (it) {
                '[', ']', ':', ',', '\\' -> {
                    append('\\')
                    append(it)
                }

                '\n' if escapeLineFeeds -> append("\\n")
                '\r' if escapeLineFeeds -> append("\\r")
                else -> append(it)
            }
        }
    }

    private fun String.unescapeIdentifiers(unescapeLineFeeds: Boolean = true) = buildString {
        val reader = StringReader(this@unescapeIdentifiers)
        while (!reader.isEnd) {
            val char = reader.readChar()
            if (char == '\\' && !reader.isEnd) {
                when (val nextChar = reader.readChar()) {
                    'n' if unescapeLineFeeds -> append('\n')
                    'r' if unescapeLineFeeds -> append('\r')
                    '[', ']', ':', ',', '\\' -> append(nextChar)
                    else -> {
                        append(char)
                        append(nextChar)
                    }
                }
            } else {
                append(char)
            }
        }
    }

    fun escapeMessageFormatIdentifiers(message: String, escapeLineFeeds: Boolean = true): String {
        return message.escapeIdentifiers(escapeLineFeeds)
    }

    fun unescapeMessageFormatIdentifiers(message: String, unescapeLineFeeds: Boolean = true): String {
        return message.unescapeIdentifiers(unescapeLineFeeds)
    }

    private suspend fun messageElementToReadableString(element: Message.Element, context: Actor? = null): String {
        if (element is PlainText) {
            return element.text
        }

        return kotlin.runCatching {
            elementFormatters.first { it.formatClass.isInstance(element) }
                .toReadableString(element, context)
        }.getOrElse {
            logger.warn { "Unknown element: $element" }
            ""
        }
    }

    suspend fun messageToReadableString(message: Message, context: Actor? = null): String {
        return when (message) {
            is Message.Element -> messageElementToReadableString(message)
            is Messages -> message.map { messageElementToReadableString(it, context) }.joinToString("")
            else -> message.toString()
        }
    }

    suspend fun messageElementToSerializedList(
        element: Message.Element,
        context: Actor? = null,
    ): List<String>? {
        val formatter = elementFormatters.firstOrNull { it.formatClass.isInstance(element) } ?: run {
            logger.warn { "Unknown element: $element" }
            return null
        }
        return runCatching {
            formatter.serialize(element, context)
        }.getOrElse {
            logger.error(it) { "Error while serializing message element with ${formatter.formatClass}: $element" }
            null
        }
    }

    suspend fun messageElementToSerialized(
        element: Message.Element,
        context: Actor? = null,
        escapeLineFeeds: Boolean = true
    ): SerializedMessage {
        if (element is PlainText) {
            return element.text.escapeIdentifiers(escapeLineFeeds)
        }

        return messageElementToSerializedList(element, context)
            ?.joinToString(":", "[", "]") { it.escapeIdentifiers(escapeLineFeeds) }
            ?: ""
    }

    suspend fun messageToSerialized(
        message: Message,
        context: Actor? = null,
        escapeLineFeeds: Boolean = true
    ): SerializedMessage {
        return message.asMessages().map { messageElementToSerialized(it, context, escapeLineFeeds) }.joinToString("")
    }

    suspend fun deserializeMessageElementSegmentList(
        segments: List<String>,
        context: Actor? = null
    ): Message.Element? {
        if (segments.isEmpty()) {
            logger.debug { "Empty segment list." }
            return null
        }

        val formatter = elementFormatters.firstOrNull { it.type == segments[0] } ?: run {
            logger.warn { "Unknown element segments: $segments" }
            return null
        }

        return runCatching {
            formatter.deserialize(segments, context)
        }.getOrElse {
            logger.error(it) { "Error while deserializing message element with type ${formatter.type}: $segments" }
            null
        }
    }

    suspend fun deserializeMessageElement(
        element: SerializedMessage,
        context: Actor? = null,
        unescapeLineFeeds: Boolean = true
    ): Message.Element {
        if (!(element.startsWith("[") && element.endsWith("]"))) {
            return element
                .unescapeIdentifiers(unescapeLineFeeds)
                .toText()
        }
        val segments = element.removeSurrounding("[", "]")
            .splitUnescaped(':')
            .map { it.unescapeIdentifiers(unescapeLineFeeds) }
        return deserializeMessageElementSegmentList(segments, context) ?: "".toText()
    }

    suspend fun deserializeMessage(
        message: SerializedMessage,
        context: Actor? = null,
        unescapeLineFeeds: Boolean = true
    ): Message {
        return message.splitByUnescapedPaired('[', ']')
            .map { deserializeMessageElement(it, context, unescapeLineFeeds) }
            .toMessages()
    }
}

fun String.escapeMessageFormatIdentifiers(escapeLineFeeds: Boolean = true) =
    MessageFormatter.escapeMessageFormatIdentifiers(this, escapeLineFeeds)

fun String.unescapeMessageFormatIdentifiers(unescapeLineFeeds: Boolean = true) =
    MessageFormatter.unescapeMessageFormatIdentifiers(this, unescapeLineFeeds)

suspend inline fun Message.toReadableString(context: Actor? = null) =
    MessageFormatter.messageToReadableString(this, context)

suspend inline fun Message.toSerialized(context: Actor? = null, escapeLineFeeds: Boolean = true) =
    MessageFormatter.messageToSerialized(this, context, escapeLineFeeds)

suspend inline fun SerializedMessage.deserializeToMessage(context: Actor? = null, unescapeLineFeeds: Boolean = true) =
    MessageFormatter.deserializeMessage(this, context, unescapeLineFeeds)

fun Messages.removeReferencePrefix() = dropWhile {
    it is MessageReference ||
            (it is OneBotMessageSegmentElement && it.segment is OneBotReply) ||
            it is MentionMessage ||
            (it is PlainText && it.text.isBlank())
}.mapIndexed { index, element ->
    if (index > 0 || element !is PlainText) {
        return@mapIndexed element
    }
    element.text.trimStart().toText()
}.toMessages()

fun Messages.standardize() = map {
    if (it is OneBotMessageSegmentElement && it.segment is OneBotReply) {
        return@map it.segment as OneBotReply
    }
    it
}.toMessages()