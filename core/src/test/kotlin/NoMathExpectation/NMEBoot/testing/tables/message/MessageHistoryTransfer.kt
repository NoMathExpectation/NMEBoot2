package NoMathExpectation.NMEBoot.testing.tables.message

import NoMathExpectation.NMEBoot.database.DatabaseManager
import NoMathExpectation.NMEBoot.database.migration.DatabaseMigration
import NoMathExpectation.NMEBoot.user.UIDManager
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createParentDirectories
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.time.Duration.Companion.days

class MessageHistoryTransfer {
    init {
        runBlocking { DatabaseManager.init() }
    }

    @Test
    @Ignore
    fun testTransferDatabase() = runTest(timeout = 1.days) {
        DatabaseMigration.migrateLegacyMessageHistory("jdbc:sqlite:data/legacy_sqlite.db", -1)
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    @Ignore
    fun testTransferLog() = runTest(timeout = 1.days) {
        val logPath = Path("logs")
        val tempLogPath = Path("data/temp/logs")
        tempLogPath.toFile().deleteOnExit()

        logPath.copyToRecursively(
            tempLogPath.createParentDirectories(),
            { _, _, e -> throw e },
            followLinks = true,
            overwrite = true
        )

        val botIdToSender = mapOf(
            "foo" to "bar"
        )
        UIDManager.fromId("onebot-foo")
        UIDManager.fromId("kook-bar")
        DatabaseMigration.migrateConsoleLogMessageHistory(botIdToSender)
    }
}