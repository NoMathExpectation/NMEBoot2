package NoMathExpectation.NMEBoot.util

import NoMathExpectation.NMEBoot.message.ComposedMessageReceipt
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.suspendCancellableCoroutine
import love.forte.simbot.bot.Bot
import love.forte.simbot.common.atomic.atomicRef
import love.forte.simbot.common.atomic.update
import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.common.time.Timestamp
import love.forte.simbot.component.kook.bot.KookBot
import love.forte.simbot.component.onebot.v11.message.OneBotMessageReceipt
import love.forte.simbot.definition.*
import love.forte.simbot.event.Event
import love.forte.simbot.event.EventListenerRegistrar
import love.forte.simbot.event.EventListenerRegistrationHandle
import love.forte.simbot.event.process
import love.forte.simbot.message.*
import kotlin.coroutines.resumeWithException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val logger = KotlinLogging.logger { }

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
        is OneBotMessageReceipt -> listOf(messageId)
        is ComposedMessageReceipt -> flatMap { it.ids ?: emptyList() }
        is Iterable<*> if firstOrNull() is MessageReceipt -> flatMap { (it as? MessageReceipt)?.ids ?: emptyList() }
        else -> run {
            logger.warn { "Cannot resolve ids for message receipt: $this" }
            null
        }
    }

val Bot.platformId
    get() = when (this) {
        is KookBot -> sourceBot.botUserInfo.id.ID
        else -> id
    }

suspend inline fun <reified E : Event> EventListenerRegistrar.awaitNextEvent(crossinline predicate: suspend (E) -> Boolean): E =
    suspendCancellableCoroutine { cont ->
        val handle = atomicRef<EventListenerRegistrationHandle?>(null)
        cont.invokeOnCancellation {
            handle.update { null }?.dispose()
        }
        handle.value = process<E> { event ->
            val result = runCatching {
                predicate(event)
            }.getOrElse {
                handle.update { null }?.dispose()
                cont.resumeWithException(it)
                return@process
            }
            if (result) {
                handle.update { null }?.let {
                    it.dispose()
                    cont.resume(event) { _, _, _ -> }
                }
            }
        }
    }