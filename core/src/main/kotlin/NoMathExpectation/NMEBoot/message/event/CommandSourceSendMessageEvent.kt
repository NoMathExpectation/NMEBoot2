@file:OptIn(FuzzyEventTypeImplementation::class)

package NoMathExpectation.NMEBoot.message.event

import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import love.forte.simbot.annotations.ExperimentalSimbotAPI
import love.forte.simbot.common.atomic.atomic
import love.forte.simbot.common.id.IntID.Companion.ID
import love.forte.simbot.common.time.Timestamp
import love.forte.simbot.event.*
import love.forte.simbot.message.MessageReceipt


interface CommandSourceSendMessageEvent<out T> : InternalMessageInteractionEvent {
    override val content: CommandSource<T>

    override suspend fun target() = content.subject
}

data class CommandSourcePreSendEvent<out T>(
    override val content: CommandSource<T>,
    override val message: InteractionMessage,
) : CommandSourceSendMessageEvent<T>, InternalMessagePreSendEvent {
    override val id = currentId.getAndIncrement().ID

    override var currentMessage = message

    @OptIn(ExperimentalSimbotAPI::class)
    override val time = Timestamp.now()

    companion object {
        val currentId = atomic(0)
    }
}

data class CommandSourcePostSendEvent<out T>(
    override val content: CommandSource<T>,
    override val message: InteractionMessage,
    override val receipt: MessageReceipt,
) : CommandSourceSendMessageEvent<T>, InternalMessagePostSendEvent {
    override val id = currentId.getAndIncrement().ID

    @OptIn(ExperimentalSimbotAPI::class)
    override val time = Timestamp.now()

    companion object {
        val currentId = atomic(0)
    }
}