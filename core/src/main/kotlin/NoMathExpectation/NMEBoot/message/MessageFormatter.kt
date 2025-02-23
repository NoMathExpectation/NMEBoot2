package NoMathExpectation.NMEBoot.message

import NoMathExpectation.NMEBoot.message.format.SerializedMessage
import NoMathExpectation.NMEBoot.util.asMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegmentElement
import love.forte.simbot.component.onebot.v11.message.segment.OneBotReply
import love.forte.simbot.message.*

private val logger = KotlinLogging.logger { }

object MessageFormatter {
    private fun messageElementToReadableString(element: Message.Element): String {
        return when (element) {
            is PlainText -> element.text
            is At -> element.originContent
            is MentionMessage -> "@$element"
//            is UrlAwareImage -> "[图片:${element.url}]"
//            is IDAwareImage -> "[图片:${element.id}]"
//            is OfflineImage -> "[离线图片]"
            is Image -> "[图片]"
            is Emoji -> "[emoji:${element.id}]"
            is Face -> "[表情:${element.id}]"
            is EmoticonMessage -> "[emoticon:$element]"
            else -> element.toString()
        }
    }

    fun messageToReadableString(message: Message): String {
        return when (message) {
            is Message.Element -> messageElementToReadableString(message)
            is Messages -> message.joinToString("") { messageElementToReadableString(it) }
            else -> message.toString()
        }
    }

    fun messageElementToSerialized(element: Message.Element): SerializedMessage {
        return when (element) {
            is PlainText -> element.text
            is At -> "[at:${element.type}:${element.target}]"
            is AtAll -> "[atAll]"
            is IDAwareImage -> "[image:id:${element.id}]"
            is UrlAwareMessage -> "[image:url:${element.url}]"
            is Emoji -> "[emoji:${element.id}]"
            is Face -> "[face:${element.id}]"
            is MessageReference -> "ref:${element.id}"

            else -> {
                logger.warn { "Unable to serialize the following message element: $element" }
                ""
            }
        }
    }

    fun messageToSerialized(message: Message): SerializedMessage {
        return message.asMessages().joinToString("") { messageElementToSerialized(it) }
    }
}

fun Message.toReadableString() = MessageFormatter.messageToReadableString(this)

fun Message.toSerialized() = MessageFormatter.messageToSerialized(this)

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