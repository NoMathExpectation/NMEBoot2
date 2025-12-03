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

    private fun String.escapeIdentifiers() = buildString {
        this@escapeIdentifiers.forEach {
            when (it) {
                '[', ']', ':', ',', '\\' -> {
                    append('\\')
                    append(it)
                }

                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(it)
            }
        }
    }

    private fun String.unescapeIdentifiers() = buildString {
        val reader = StringReader(this@unescapeIdentifiers)
        while (!reader.isEnd) {
            val char = reader.readChar()
            if (char == '\\' && !reader.isEnd) {
                when (val nextChar = reader.readChar()) {
                    'n' -> append('\n')
                    'r' -> append('\r')
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

    fun escapeMessageFormatIdentifiers(message: String): String {
        return message.escapeIdentifiers()
    }

    fun unescapeMessageFormatIdentifiers(message: String): String {
        return message.unescapeIdentifiers()
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

    suspend fun messageElementToSerialized(element: Message.Element, context: Actor? = null): SerializedMessage {
        if (element is PlainText) {
            return element.text.escapeIdentifiers()
        }

        return kotlin.runCatching {
            elementFormatters.first { it.formatClass.isInstance(element) }
                .serialize(element, context)
                .joinToString(":", "[", "]") { it.escapeIdentifiers() }
        }.getOrElse {
            logger.warn { "Unknown element: $element" }
            ""
        }
    }

    suspend fun messageToSerialized(message: Message, context: Actor? = null): SerializedMessage {
        return message.asMessages().map { messageElementToSerialized(it, context) }.joinToString("")
    }

    suspend fun deserializeMessageElement(element: SerializedMessage, context: Actor? = null): Message.Element {
        if (!(element.startsWith("[") && element.endsWith("]"))) {
            return element
                .unescapeIdentifiers()
                .toText()
        }
        val segments = element.removeSurrounding("[", "]")
            .splitUnescaped(':')
            .map { it.unescapeIdentifiers() }
        if (segments.isEmpty()) {
            return "".toText()
        }
        val type = segments[0]
        return kotlin.runCatching {
            elementFormatters.first { it.type == type }
                .deserialize(segments, context)
        }.getOrElse {
            logger.warn { "Unknown element: $element" }
            "".toText()
        }
    }

    suspend fun deserializeMessage(message: SerializedMessage, context: Actor? = null): Message {
        return message.splitByUnescapedPaired('[', ']')
            .map { deserializeMessageElement(it, context) }
            .toMessages()
    }
}

fun String.escapeMessageFormatIdentifiers() = MessageFormatter.escapeMessageFormatIdentifiers(this)

fun String.unescapeMessageFormatIdentifiers() = MessageFormatter.unescapeMessageFormatIdentifiers(this)

suspend inline fun Message.toReadableString(context: Actor? = null) =
    MessageFormatter.messageToReadableString(this, context)

suspend inline fun Message.toSerialized(context: Actor? = null) = MessageFormatter.messageToSerialized(this, context)

suspend inline fun SerializedMessage.deserializeToMessage(context: Actor? = null) =
    MessageFormatter.deserializeMessage(this, context)

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