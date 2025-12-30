@file:OptIn(ExperimentalTime::class)

package NoMathExpectation.NMEBoot.database.message

import NoMathExpectation.NMEBoot.command.impl.commandConfig
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.database.migration.DatabaseMigration
import NoMathExpectation.NMEBoot.database.migration.DatabaseMigration.isMigrating
import NoMathExpectation.NMEBoot.message.deserializeToMessage
import NoMathExpectation.NMEBoot.message.event.CommandSourcePostSendEvent
import NoMathExpectation.NMEBoot.message.message
import NoMathExpectation.NMEBoot.message.standardize
import NoMathExpectation.NMEBoot.message.toSerialized
import NoMathExpectation.NMEBoot.util.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import love.forte.simbot.definition.Actor
import love.forte.simbot.event.ActorEvent
import love.forte.simbot.event.InternalMessagePostSendEvent
import love.forte.simbot.event.MessageEvent
import love.forte.simbot.message.Messages
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object MessageHistoryTable : LongIdTable() {
    val platform = varchar("platform", 128)
    val botId = varchar("botId", 1024).nullable()

    val globalSubjectId = varchar("globalSubject", 1024).nullable()
    val globalSubjectName = varchar("globalSubjectName", 1024).nullable()

    val subjectId = varchar("subject", 1024).nullable()
    val subjectName = varchar("subjectName", 1024).nullable()

    val senderId = varchar("senderId", 1024)
    val senderUid = long("senderUid")
    val senderName = varchar("senderName", 1024)

    val messageId = varchar("messageId", 512).nullable()
    val binaryMessage = binary("message")
    val message = binaryMessage.transform(
        { it.decodeToString() }, { it.encodeToByteArray() }
    )

    @OptIn(ExperimentalTime::class)
    val time = timestamp("time").clientDefault { Clock.System.now() }

    val isBot = bool("isBot").default(false)

    val version = integer("version").default(MessageHistory.CURRENT_VERSION)
}

class MessageHistory(id: EntityID<Long>) : LongEntity(id) {
    var platform by MessageHistoryTable.platform
    var botId by MessageHistoryTable.botId

    var globalSubjectId by MessageHistoryTable.globalSubjectId
    var globalSubjectName by MessageHistoryTable.globalSubjectName

    var subjectId by MessageHistoryTable.subjectId
    var subjectName by MessageHistoryTable.subjectName

    var senderId by MessageHistoryTable.senderId
    var senderUid by MessageHistoryTable.senderUid
    var senderName by MessageHistoryTable.senderName

    var messageId by MessageHistoryTable.messageId
    var binaryMessage by MessageHistoryTable.binaryMessage
    var message by MessageHistoryTable.message

    @OptIn(ExperimentalTime::class)
    var time by MessageHistoryTable.time

    var isBot by MessageHistoryTable.isBot

    var version by MessageHistoryTable.version

    companion object : LongEntityClass<MessageHistory>(MessageHistoryTable) {
        const val CURRENT_VERSION = 3

        private val logger = KotlinLogging.logger { }

        private val messageCache =
            Channel<Triple<MessageEvent, CommandSource<*>?, Instant>>(capacity = Channel.UNLIMITED)
        private val postSendMessageCache =
            Channel<Pair<InternalMessagePostSendEvent, Instant>>(capacity = Channel.UNLIMITED)

        private val postMigrationMutex = Mutex()

        suspend fun logMessage(
            event: MessageEvent,
            source: CommandSource<*>?,
            noLockingMutex: Boolean = false,
            time: Instant = Clock.System.now()
        ) {
            if (source == null) {
                logger.warn { "Command source is null: $event" }
            }

            if (DatabaseMigration.migrationFailed.value) {
                return
            }

            if (isMigrating.value || DatabaseMigration.getMigrationEnabled()) {
                messageCache.send(Triple(event, source, time))
                return
            }

            if (!noLockingMutex) {
                postMigrationMutex.lock()
            }
            try {
                val message =
                    event.messageContent.messages.standardize().toSerialized((event as? ActorEvent)?.content())

                transaction {
                    new {
                        platform = source?.platform ?: "<unknown>"
                        botId = event.bot.platformId.toString()

                        globalSubjectId = source?.globalSubject?.id?.toString()
                        globalSubjectName = source?.globalSubject?.name

                        subjectId = source?.subject?.id?.toString()
                        subjectName = source?.subject?.name

                        senderId = event.authorId.toString()
                        senderUid = source?.uid ?: -1
                        senderName = source?.executor?.nickOrName ?: "<unknown>"

                        messageId = event.messageContent.id.toString()
                        this.message = message

                        this.time = time
                    }
                }
            } finally {
                if (postMigrationMutex.isLocked) {
                    postMigrationMutex.unlock()
                }
            }
        }

        suspend fun logPostSendMessage(
            event: InternalMessagePostSendEvent,
            noLockingMutex: Boolean = false,
            time: Instant = Clock.System.now()
        ) {
            if (event !is CommandSourcePostSendEvent<*>) {
                return
            }

            if (DatabaseMigration.migrationFailed.value) {
                return
            }

            if (isMigrating.value || DatabaseMigration.getMigrationEnabled()) {
                postSendMessageCache.send(event to time)
                return
            }

            if (!noLockingMutex) {
                postMigrationMutex.lock()
            }
            try {
                val source = event.content
                val msgString = event.message.message.toSerialized(event.target())
                val botSource = source.botAsSource()

                transaction {
                    new {
                        platform = source.platform
                        botId = source.bot?.platformId?.toString()

                        globalSubjectId = source.globalSubject?.id?.toString()
                        globalSubjectName = source.globalSubject?.name

                        subjectId = source.subject?.id?.toString()
                        subjectName = source.subject?.name

                        senderId = botId ?: "<unknown>"
                        senderUid = botSource?.uid ?: -1
                        senderName = botSource?.executor?.nickOrName ?: "<unknown>"

                        val ids = event.receipt.ids
                        messageId = when {
                            ids == null -> null
                            ids.size == 1 -> ids.first().toString()
                            else -> ids.joinToString(", ", "[", "]")
                        }
                        message = msgString

                        isBot = true

                        this.time = time
                    }
                }
            } finally {
                if (postMigrationMutex.isLocked) {
                    postMigrationMutex.unlock()
                }
            }
        }

        suspend fun handleCachedMessages() {
            postMigrationMutex.lock()
            try {
                isMigrating.getAndSet(false)

                while (true) {
                    val (event, source, time) = messageCache.tryReceive().getOrNull() ?: break
                    logMessage(event, source, true, time)
                }

                while (true) {
                    val (event, time) = postSendMessageCache.tryReceive().getOrNull() ?: break
                    logPostSendMessage(event, true, time)
                }
            } finally {
                if (postMigrationMutex.isLocked) {
                    postMigrationMutex.unlock()
                }
            }
        }

        suspend fun fetchRandomMessage(
            platform: String,
            globalSubjectId: String,
            messageDeserializeContext: Actor?
        ): Pair<String, Messages>? {
            val commandPrefix = commandConfig.get().commandPrefix
            return transaction {
                MessageHistoryTable.select(MessageHistoryTable.senderName, MessageHistoryTable.message).where {
                    (MessageHistoryTable.platform eq platform) and
                            (MessageHistoryTable.globalSubjectId eq globalSubjectId) and
                            (MessageHistoryTable.isBot eq false) and
                            (MessageHistoryTable.binaryMessage notLike "$commandPrefix%") and
                            (MessageHistoryTable.message neq "")
                }.orderBy(Random() to SortOrder.ASC)
                    .limit(1)
                    .firstOrNull()
                    ?.let {
                        it[MessageHistoryTable.senderName] to it[MessageHistoryTable.message]
                    }
            }?.let {
                logger.info { "Fetched history: $it" }
                it.first to it.second.deserializeToMessage(messageDeserializeContext)
            }
        }
    }
}