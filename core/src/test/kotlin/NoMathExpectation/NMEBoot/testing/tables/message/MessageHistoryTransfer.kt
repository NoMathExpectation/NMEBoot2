package NoMathExpectation.NMEBoot.testing.tables.message

import NoMathExpectation.NMEBoot.database.DatabaseManager
import NoMathExpectation.NMEBoot.database.migration.DatabaseMigration
import NoMathExpectation.NMEBoot.user.UIDManager
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.Duration.Companion.days

class MessageHistoryTransfer {
    init {
        DatabaseManager.init()
    }

    @Test
    @Ignore
    fun testTransferDatabase() = runTest(timeout = 1.days) {
        DatabaseMigration.migrateLegacyMessageHistory("jdbc:sqlite:data/legacy_sqlite.db", -1)
    }

    @Test
    @Ignore
    fun testTransferLog() = runTest(timeout = 1.days) {
        val botIdToSender = mapOf(
            "foo" to "bar"
        )
        UIDManager.fromId("onebot-foo")
        UIDManager.fromId("kook-bar")
        DatabaseMigration.migrateConsoleLogMessageHistory(botIdToSender)
    }
}