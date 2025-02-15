package NoMathExpectation.NMEBoot.message.onebot

import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.component.onebot.v11.core.event.notice.OneBotGroupUploadEvent
import love.forte.simbot.component.onebot.v11.event.notice.RawGroupUploadEvent

object OneBotFileCache {
    private val perUserCache = mutableMapOf<Pair<Long, Long>, MutableList<RawGroupUploadEvent.FileInfo>>()
    private val perGroupCache = mutableMapOf<Long, MutableList<RawGroupUploadEvent.FileInfo>>()

    private val logger = KotlinLogging.logger { }

    const val MAX_CACHE = 10

    fun record(groupId: Long, uploaderId: Long, info: RawGroupUploadEvent.FileInfo) {
        val perUserList = perUserCache.getOrPut(groupId to uploaderId) { mutableListOf() }
        if (perUserList.size >= MAX_CACHE) {
            val removed = perUserList.removeAt(0)
            logger.debug { "Removing expired per user cache $removed" }
        }
        perUserList.add(info)

        val perGroupList = perGroupCache.getOrPut(groupId) { mutableListOf() }
        if (perGroupList.size >= MAX_CACHE) {
            val removed = perGroupList.removeAt(0)
            logger.debug { "Removing expired per group cache $removed" }
        }
        perGroupList.add(info)

        logger.debug { "Adding new cache $info" }
    }

    fun record(event: RawGroupUploadEvent) {
        record(event.groupId.value, event.userId.value, event.file)
    }

    fun record(event: OneBotGroupUploadEvent) {
        record(event.groupId.value, event.userId.value, event.fileInfo)
    }

    operator fun get(groupId: Long, uploaderId: Long, keyword: String?): RawGroupUploadEvent.FileInfo? {
        val list = perUserCache[groupId to uploaderId] ?: return null
        if (keyword == null) {
            return list.lastOrNull()
        }
        return list.lastOrNull { keyword.lowercase() in it.name.lowercase() }
    }

    operator fun get(groupId: Long, keyword: String?): RawGroupUploadEvent.FileInfo? {
        val list = perGroupCache[groupId] ?: return null
        if (keyword == null) {
            return list.lastOrNull()
        }
        return list.lastOrNull { keyword.lowercase() in it.name.lowercase() }
    }
}