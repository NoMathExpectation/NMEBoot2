package NoMathExpectation.NMEBoot.util

import love.forte.simbot.definition.Member
import love.forte.simbot.message.Message
import love.forte.simbot.message.Messages
import love.forte.simbot.message.messagesOf

val Member.nickOrName: String
    get() = nick ?: name

fun Message.asMessages() = when (this) {
    is Message.Element -> messagesOf(this)
    is Messages -> this
    else -> error("Unsupported message type: $this")
}