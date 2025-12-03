package NoMathExpectation.NMEBoot.database.message

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

object LegacyMessageHistoryTable : LongIdTable("MessageHistory") {
    val ids = text("ids", eagerLoading = true).default("")
    val sender = long("sender")
    val group = long("group").nullable()
    val name = varchar("name", 127)
    val message = text("message", eagerLoading = true)
    val time = long("time")
}

class LegacyMessageHistory(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<LegacyMessageHistory>(LegacyMessageHistoryTable)

    var ids by LegacyMessageHistoryTable.ids
    var sender by LegacyMessageHistoryTable.sender
    var group by LegacyMessageHistoryTable.group
    var name by LegacyMessageHistoryTable.name
    var message by LegacyMessageHistoryTable.message
    var time by LegacyMessageHistoryTable.time
}