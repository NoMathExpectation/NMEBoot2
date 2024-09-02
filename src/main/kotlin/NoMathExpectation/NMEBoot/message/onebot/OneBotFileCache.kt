package NoMathExpectation.NMEBoot.message.onebot

import love.forte.simbot.component.onebot.v11.core.event.notice.OneBotGroupUploadEvent
import love.forte.simbot.component.onebot.v11.event.notice.RawGroupUploadEvent

object OneBotFileCache {
    val cache = mutableMapOf<Pair<Long, Long>, MutableList<RawGroupUploadEvent.FileInfo>>()

    const val MAX_CACHE = 10

    fun record(groupId: Long, uploaderId: Long, info: RawGroupUploadEvent.FileInfo) {
        val list = cache.getOrPut(groupId to uploaderId) { mutableListOf() }
        if (list.size >= MAX_CACHE) {
            list.removeAt(0)
        }
        list.add(info)
    }

    fun record(event: RawGroupUploadEvent) {
        record(event.groupId.value, event.userId.value, event.file)
    }

    fun record(event: OneBotGroupUploadEvent) {
        record(event.groupId.value, event.userId.value, event.fileInfo)
    }

    operator fun get(groupId: Long, uploaderId: Long, keyword: String?): RawGroupUploadEvent.FileInfo? {
        val list = cache[groupId to uploaderId] ?: return null
        if (keyword == null) {
            return list.lastOrNull()
        }
        return list.lastOrNull { it.name.lowercase().contains(keyword.lowercase()) }
    }
}