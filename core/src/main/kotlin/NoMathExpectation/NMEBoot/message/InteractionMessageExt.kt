package NoMathExpectation.NMEBoot.message

import love.forte.simbot.event.InteractionMessage
import love.forte.simbot.message.toText

val InteractionMessage.message
    get() = when (this) {
        is InteractionMessage.Text -> text.toText()
        is InteractionMessage.Message -> message
        is InteractionMessage.MessageContent -> messageContent.messages
        else -> error("Unsupported InteractionMessage: $this")
    }