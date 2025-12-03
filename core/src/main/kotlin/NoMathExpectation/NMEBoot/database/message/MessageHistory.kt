package NoMathExpectation.NMEBoot.database.message

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object MessageHistoryTable : LongIdTable() {
    val platform = varchar("platform", 128)
    val botId = varchar("botId", 1024)

    val globalSubjectId = varchar("globalSubject", 1024).nullable()
    val globalSubjectName = varchar("globalSubjectName", 1024).nullable()

    val subjectId = varchar("subject", 1024).nullable()
    val subjectName = varchar("subjectName", 1024).nullable()

    val senderId = varchar("senderId", 1024)
    val senderUid = long("senderUid")
    val senderName = varchar("senderName", 1024)

    val messageId = varchar("messageId", 512).nullable()
    val message = text("message")

    @OptIn(ExperimentalTime::class)
    val time = timestamp("time").clientDefault { Clock.System.now() }

    val isBot = bool("isBot").default(false)

    val version = integer("version").default(MessageHistory.CURRENT_VERSION)
}

class MessageHistory(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<MessageHistory>(MessageHistoryTable) {
        val CURRENT_VERSION = 3
    }

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
    var message by MessageHistoryTable.message

    @OptIn(ExperimentalTime::class)
    var time by MessageHistoryTable.time

    var isBot by MessageHistoryTable.isBot

    var version by MessageHistoryTable.version
}