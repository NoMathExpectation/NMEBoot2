package NoMathExpectation.NMEBoot.message.onebot

import NoMathExpectation.NMEBoot.message.onebot.OneBotFolding.FoldIgnore
import NoMathExpectation.NMEBoot.message.onebot.apiExt.extApi
import NoMathExpectation.NMEBoot.message.toReadableString
import NoMathExpectation.NMEBoot.util.asMessages
import NoMathExpectation.NMEBoot.util.nickOrName
import NoMathExpectation.NMEBoot.util.storageOf
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.common.id.toLongID
import love.forte.simbot.component.onebot.common.annotations.ExperimentalOneBotAPI
import love.forte.simbot.component.onebot.common.annotations.InternalOneBotAPI
import love.forte.simbot.component.onebot.v11.core.actor.OneBotGroup
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.message.resolveToOneBotSegmentList
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForward
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegmentElement
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageReceipt
import love.forte.simbot.message.toMessages

internal object OneBotFolding {
    @Serializable
    data object FoldIgnore : Message.Element {
        override fun toString() = "[Folding Ignored]"
    }

    @Serializable
    private data class Config(
        val minLines: Int = 10,
        val minLength: Int = 150,
    )

    private val configStore = storageOf("config/folding.json", Config())

    private fun postProcess(msg: Message) = msg.removeFoldIgnore()

    @OptIn(InternalOneBotAPI::class, ExperimentalOneBotAPI::class)
    internal suspend fun processGroupFold(
        bot: OneBotBot,
        msg: Message,
        subject: OneBotGroup
    ): Pair<Message?, MessageReceipt?> {
        val messages = msg.asMessages()
        if (messages.any { it is FoldIgnore || it.containsOneBotForward() }) {
            return postProcess(msg) to null
        }

        val content = messages.toReadableString(subject)
        val config = configStore.get()
        if (content.length < config.minLength && content.count { it == '\n' } + 1 < config.minLines) {
            return postProcess(messages) to null
        }

        val receipt = bot.extApi.sendGroupForwardMsg(
            subject.id.toLongID(),
            listOf(
                OneBotForwardNode.create(
                    bot.userId.toString().ID,
                    subject.botAsMember().nickOrName,
                    messages.resolveToOneBotSegmentList(bot.configuration.defaultImageAdditionalParamsProvider)
                )
            )
        )
        return null to receipt
    }
}

fun Message.containsOneBotForward() =
    asMessages().any { it is OneBotMessageSegmentElement && (it.segment is OneBotForward || it.segment is OneBotForwardNode) }

fun Message.removeFoldIgnore() = asMessages().filter { it !is FoldIgnore }.toMessages()