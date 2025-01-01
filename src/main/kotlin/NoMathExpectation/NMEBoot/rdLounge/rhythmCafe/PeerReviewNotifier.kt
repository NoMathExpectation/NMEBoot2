package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe

import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.impl.source.offline.OfflineCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.offline.toOnlineOrNull
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.DatasetteRequest
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.LevelStatus
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.bodyToLevelStatusList
import NoMathExpectation.NMEBoot.util.storageOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.ID
import love.forte.simbot.message.At
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessagesBuilder
import love.forte.simbot.message.buildMessages

object PeerReviewNotifier {
    @Serializable
    private data class SubscribeInfo(
        val authorName: String,
        val subjectId: String?,
        val source: OfflineCommandSource,
        val mentionId: ID?,
    )

    @Serializable
    private data class Data(
        val refreshInterval: Long = 30,
        val interval: Long = 60 * 60,
        val subscribers: MutableMap<String, SubscribeInfo> = mutableMapOf(),
    )

    private val dataStorage = storageOf("data/rd/pr_subscribe.json", Data())

    suspend fun setSubscribe(source: CommandSource<*>, authorName: String) {
        dataStorage.referenceUpdate {
            it.subscribers[source.primaryPermissionId] = SubscribeInfo(
                authorName,
                source.subjectPermissionId,
                source.toOffline(),
                source.executor?.id,
            )
        }
    }

    suspend fun removeSubscribe(source: CommandSource<*>) {
        dataStorage.referenceUpdate {
            it.subscribers.remove(source.primaryPermissionId)
        }
    }

    private val pendingLevels = mutableSetOf<String>()

    private val logger = KotlinLogging.logger { }
    private var coroutineScope: CoroutineScope? = null

    private fun MessagesBuilder.formatNotification(mentions: List<ID?>, level: LevelStatus) {
        mentions.forEach {
            it?.let { id ->
                +At(id)
                +" "
            }
        }
        +"谱面 "
        +level.song
        +" "
        +if (level.isApproved) "已经" else "未能"
        +"通过pr\n"
    }

    private suspend fun refreshRoutine() {
        logger.info { "开始刷新谱面待审名单" }

        pendingLevels.clear()
        RhythmCafeSearchEngine.datasetteQuery(DatasetteRequest.ofPending())
            .bodyToLevelStatusList()
            .forEach { pendingLevels += it.id }

        logger.info { "刷新完成" }
    }

    private suspend fun notifyRoutine() {
        logger.info { "开始通知订阅者" }

        val data = dataStorage.get()
        val subjects = data.subscribers
            .values
            .groupBy { it.subjectId }
        val authors = data.subscribers
            .values
            .groupBy { it.authorName }

        val subjectNotifications = mutableMapOf<String, MessagesBuilder>()
        val independentNotifications = mutableListOf<Pair<SubscribeInfo, Message>>()

        RhythmCafeSearchEngine.datasetteQuery(DatasetteRequest.ofIds(*pendingLevels.toTypedArray()))
            .bodyToLevelStatusList()
            .filterNot { it.isPending }
            .forEach { level ->
                level.authors
                    .flatMap { authors[it] ?: listOf() }
                    .groupBy { it.subjectId }
                    .forEach msgGen@{ (subjectId, subscribers) ->
                        if (subjectId == null) {
                            subscribers.forEach {
                                independentNotifications += it to buildMessages {
                                    formatNotification(listOf(it.mentionId), level)
                                }
                            }
                            return@msgGen
                        }

                        subjectNotifications.getOrPut(subjectId) {
                            MessagesBuilder.create()
                        }.formatNotification(
                            subscribers.map { it.mentionId },
                            level,
                        )
                    }
            }

        subjectNotifications.forEach { (subjectId, builder) ->
            runCatching {
                subjects[subjectId]
                    ?.asFlow()
                    ?.mapNotNull { it.source.toOnlineOrNull() }
                    ?.firstOrNull()
                    ?.send(builder.build())
                    ?: logger.warn { "无法找到可用的群发对象 $subjectId" }
            }.onFailure {
                logger.error(it) { "发送 $subjectId 的消息失败" }
            }
        }
        independentNotifications.forEach { (subscriber, message) ->
            runCatching {
                subscriber.source
                    .toOnlineOrNull()
                    ?.send(message)
                    ?: logger.warn { "无法找到可用的独立发送对象 ${subscriber.authorName}" }
            }.onFailure {
                logger.error(it) { "发送 ${subscriber.authorName} 的消息失败" }
            }
        }

        logger.info { "通知完成" }
    }

    suspend fun start() {
        stop()
        logger.info { "启动协程中..." }
        coroutineScope = CoroutineScope(currentCoroutineContext())
        coroutineScope!!.launch {
            val data = dataStorage.get()
            withContext(Dispatchers.IO) {
                while (isActive) {
                    var error = false
                    kotlin.runCatching {
                        refreshRoutine()
                    }.onFailure {
                        error = true
                        logger.error(it) { "刷新待审谱面名单失败，将在${data.refreshInterval}秒后重试" }
                        delay(data.refreshInterval * 1000)
                    }

                    if (error) {
                        continue
                    }

                    delay(data.interval * 1000)

                    while (true) {
                        error = false
                        kotlin.runCatching {
                            notifyRoutine()
                        }.onFailure {
                            error = true
                            logger.error(it) { "通知订阅者失败，将在${data.interval}秒后重试" }
                            delay(data.interval * 1000)
                        }

                        if (!error) {
                            break
                        }
                    }
                }
            }
        }
        logger.info { "启动完成" }
    }

    fun stop() {
        logger.info { "销毁协程中..." }
        coroutineScope?.cancel()
        logger.info { "销毁完成" }
    }
}