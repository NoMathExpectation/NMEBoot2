package NoMathExpectation.NMEBoot.message.onebot

import NoMathExpectation.NMEBoot.message.toReadableString
import NoMathExpectation.NMEBoot.util.asMessages
import NoMathExpectation.NMEBoot.util.storageOf
import kotlinx.serialization.Serializable
import love.forte.simbot.component.onebot.common.annotations.ExperimentalOneBotAPI
import love.forte.simbot.component.onebot.common.annotations.InternalOneBotAPI
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.message.resolveToOneBotSegmentList
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForward
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegmentElement
import love.forte.simbot.component.onebot.v11.message.segment.toElement
import love.forte.simbot.message.Message
import love.forte.simbot.message.messagesOf
import love.forte.simbot.message.toMessages

internal object OneBotFolding {
    @Serializable
    data object FoldIgnore : Message.Element {
        override fun toString() = "[Folding Ignored]"
    }

    @Serializable
    data class Config(
        val minLines: Int = 10,
        val minLength: Int = 150,
    )

    private val configStore = storageOf("config/folding.json", Config())

    private fun postProcess(msg: Message) = msg.asMessages().filter { it !is FoldIgnore }.toMessages()

    @OptIn(InternalOneBotAPI::class, ExperimentalOneBotAPI::class)
    internal suspend fun processFold(bot: OneBotBot, msg: Message, nick: String? = null): Message {
        var messages = msg.asMessages()
        if (messages.any { it is FoldIgnore || it.containsOneBotForward() }) {
            return postProcess(msg)
        }

        val content = messages.toReadableString()
        val config = configStore.get()
        if (content.length >= config.minLength || content.count { it == '\n' } + 1 >= config.minLines) {
            messages = messagesOf(
                OneBotForwardNode.create(
                    bot.userId,
                    nick ?: bot.name,
                    messages.resolveToOneBotSegmentList(bot.configuration.defaultImageAdditionalParamsProvider)
                ).toElement()
            )
        }

        return postProcess(messages)
    }
}

fun Message.containsOneBotForward() =
    asMessages().any { it is OneBotMessageSegmentElement && (it.segment is OneBotForward || it.segment is OneBotForwardNode) }