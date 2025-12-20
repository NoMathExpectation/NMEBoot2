package NoMathExpectation.NMEBoot.util

import love.forte.simbot.common.id.ID
import love.forte.simbot.common.time.Timestamp
import love.forte.simbot.definition.*
import love.forte.simbot.message.*
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val User.nickOrName: String
    get() = (this as? Member)?.nick ?: name

fun Message.asMessages() = when (this) {
    is Message.Element -> messagesOf(this)
    is Messages -> this
    else -> error("Unsupported message type: $this")
}

@OptIn(ExperimentalTime::class)
val Timestamp.instant get() = Instant.fromEpochMilliseconds(milliseconds)

val Actor.name: String?
    get() = when (this) {
        is User -> nickOrName
        is Channel -> name
        is ChatRoom -> name
        is Organization -> name
        else -> null
    }

val MessageReceipt.ids: List<ID>?
    get() = when (this) {
        is SingleMessageReceipt -> listOf(id)
        is AggregatedMessageReceipt -> map { it.id }
        else -> null
    }