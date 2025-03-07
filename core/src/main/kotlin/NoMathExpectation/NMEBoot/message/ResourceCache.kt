package NoMathExpectation.NMEBoot.message

import NoMathExpectation.NMEBoot.util.sha1String
import NoMathExpectation.NMEBoot.util.storageOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.UUID
import love.forte.simbot.resource.Resource
import love.forte.simbot.resource.toResource
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

object ResourceCache {
    private val logger = KotlinLogging.logger { }

    @Serializable
    data class Item(
        val id: ID,
        val sha1: String,
        val type: Type,
        var expires: Instant,
    ) {
        enum class Type {
            IMAGE
        }

        fun getFile() = File("data/resource_cache/$id")

        fun toResource(): Resource = getFile().toResource()
    }

    @Serializable
    private data class Data(
        val initialExpireTime: Duration = 7.days,
        val expireTimeMultiplier: Double = 1.5,
        val scanInterval: Duration = 10.minutes,
        val itemCheckPerScan: Int = 10,
        val items: MutableMap<ID, Item> = mutableMapOf(),
        val digestToItemIdMap: MutableMap<String, ID> = mutableMapOf(),
    )

    init {
        File("data/resource_cache").mkdirs()
    }

    private val cache = storageOf<Data>("data/resource_cache/cache.json", Data())

    suspend fun put(resource: Resource, type: Item.Type): Item = cache.referenceUpdate {
        val now = Clock.System.now()
        val resourceSha1 = resource.data().sha1String()

        it.digestToItemIdMap[resourceSha1]?.let { id ->
            val item = it.items[id]!!
            item.expires = now + maxOf(
                it.initialExpireTime,
                (item.expires - now) * it.expireTimeMultiplier,
            )
            logger.debug { "Resource $id cache hit, will expire at ${item.expires.epochSeconds}." }
            return@referenceUpdate item
        }

        val id = UUID.random()
        val item = Item(
            id,
            resourceSha1,
            type,
            now + it.initialExpireTime,
        )
        it.items[id] = item
        it.digestToItemIdMap[resourceSha1] = id

        item.getFile().writeBytes(resource.data())

        logger.debug { "New resource cache $id." }
        item
    }

    suspend fun get(id: ID): Item? = cache.referenceUpdate {
        val item = it.items[id] ?: run {
            logger.debug { "Resource $id cache miss." }
            return@referenceUpdate null
        }

        val now = Clock.System.now()
        item.expires = now + maxOf(
            it.initialExpireTime,
            (item.expires - now) * it.expireTimeMultiplier,
        )

        logger.debug { "Resource $id cache hit, will expire at ${item.expires.epochSeconds}." }
        return@referenceUpdate item
    }

    suspend fun remove(id: ID): Unit = cache.referenceUpdate {
        val item = it.items.remove(id) ?: return@referenceUpdate
        it.digestToItemIdMap.remove(item.sha1)
        item.getFile().delete()
        logger.debug { "Resource $id cache removed." }
    }

    private suspend fun routine() {
        delay(cache.get().scanInterval)
        val now = Clock.System.now()
        cache.referenceUpdate {
            repeat(it.itemCheckPerScan) { _ ->
                val item = it.items.values.randomOrNull() ?: return@referenceUpdate
                if (item.expires > now) {
                    return@repeat
                }

                it.items.remove(item.id)
                it.digestToItemIdMap.remove(item.sha1)
                item.getFile().delete()
                logger.debug { "Resource ${item.id} cache expired." }
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
                    logger.error(it) { "Exception on routine." }
                }
            }
        }
    }
}