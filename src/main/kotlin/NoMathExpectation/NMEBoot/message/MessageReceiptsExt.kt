package NoMathExpectation.NMEBoot.message

import love.forte.simbot.message.AggregatedMessageReceipt
import love.forte.simbot.message.MessageReceipt
import love.forte.simbot.message.SingleMessageReceipt
import love.forte.simbot.message.aggregation

fun Iterable<MessageReceipt>.aggregateAll() = flatMap {
    when (it) {
        is SingleMessageReceipt -> listOf(it)
        is AggregatedMessageReceipt -> it
        else -> error("Unknown MessageReceipt type: $it")
    }
}.aggregation()