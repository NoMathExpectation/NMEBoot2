package NoMathExpectation.NMEBoot.message

import love.forte.simbot.ability.DeleteOption
import love.forte.simbot.message.MessageReceipt

class ComposedMessageReceipt(
    private val receipts: Collection<MessageReceipt>
) : MessageReceipt, Collection<MessageReceipt> by receipts {
    override suspend fun delete(vararg options: DeleteOption) = receipts.forEach { it.delete(*options) }
}

fun Iterable<MessageReceipt>.aggregateAll() = ComposedMessageReceipt(toList())