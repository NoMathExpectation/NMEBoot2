package NoMathExpectation.NMEBoot.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class Cache<K, V>(
    path: String,
) {
    private val logger = KotlinLogging.logger { }

    @Serializable
    data class Item<V>(
        var value: V,
        var expireInstant: Instant,
    )

    private val storage = storageOf<MutableMap<K, Item<V>>>(path, mutableMapOf())

    private var expireInstantProvider: suspend (K, V, Instant?) -> Instant = { _, _, _ -> Clock.System.now() + 1.days }
    fun provideExpireInstant(provider: suspend (K, V, Instant?) -> Instant) = apply {
        expireInstantProvider = provider
    }

    private var onRemove: suspend (K, V) -> Unit = { _, _ -> }
    fun onRemove(action: suspend (K, V) -> Unit) = apply {
        onRemove = action
    }

    private suspend fun MutableMap<K, Item<V>>.getAndCheckExpiry(key: K): Item<V>? {
        val item = this[key] ?: return null
        if (item.expireInstant < Clock.System.now()) {
            logger.debug { "Cache expired: $key" }
            this -= key
            onRemove(key, item.value)
            return null
        }
        return item
    }

    suspend fun get(key: K): V? = storage.referenceUpdate {
        val item = it.getAndCheckExpiry(key) ?: run {
            logger.debug { "Cache miss: $key" }
            return@referenceUpdate null
        }
        logger.debug { "Cache hit: $key" }
        item.expireInstant = expireInstantProvider(key, item.value, item.expireInstant)
        item.value
    }

    suspend fun set(key: K, value: V): Unit = storage.referenceUpdate {
        val item = it.getAndCheckExpiry(key) ?: run {
            it[key] = Item(value, expireInstantProvider(key, value, null))
            return@referenceUpdate
        }
        logger.debug { "Cache updated: $key" }
        item.value = value
        item.expireInstant = expireInstantProvider(key, value, item.expireInstant)
    }

    var routineInterval: Duration = 1.minutes
    fun routineInterval(duration: Duration) = apply {
        routineInterval = duration
    }

    var itemToCheckPerRoutine: Int = 10
    fun itemToCheckPerRoutine(count: Int) = apply {
        itemToCheckPerRoutine = count
    }

    private suspend fun routine() {
        delay(routineInterval)

        if (storage.get().isEmpty()) {
            return
        }

        storage.referenceUpdate {
            repeat(itemToCheckPerRoutine) { _ ->
                it.keys.randomOrNull()?.let { key -> it.getAndCheckExpiry(key) } ?: return@referenceUpdate
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun launchRoutine() {
        GlobalScope.launch {
            while (isActive) {
                runCatching {
                    routine()
                }.onFailure {
                    logger.error(it) { "Exception on cache routine." }
                }
            }
        }
    }
}