package NoMathExpectation.NMEBoot.message

import love.forte.simbot.message.*

object MessageFormatter {
    private fun messageElementToReadableString(element: Message.Element): String {
        return when(element) {
            is PlainText -> element.text
            is At -> element.originContent
            is MentionMessage -> "@$element"
            is UrlAwareImage -> "[图片:${element.url}]"
            is IDAwareImage -> "[图片:${element.id}]"
            is OfflineImage -> "[离线图片]"
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
}

fun Message.toReadableString() = MessageFormatter.messageToReadableString(this)