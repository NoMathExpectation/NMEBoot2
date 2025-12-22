package NoMathExpectation.NMEBoot.database

import NoMathExpectation.NMEBoot.database.message.MessageHistoryTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.sqlite.SQLiteConfig
import java.sql.Connection
import kotlin.io.path.Path
import kotlin.io.path.copyTo

object DatabaseManager {
    private val logger = KotlinLogging.logger { }

    fun createDataSource(jdbcUrl: String) = HikariDataSource(HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
        transactionIsolation = "TRANSACTION_SERIALIZABLE"
        validate()
    })

    val mainDataSource = createDataSource("jdbc:sqlite:data/sqlite.db")
    private var _mainDatabase: Database? = null
    val mainDatabase get() = _mainDatabase ?: error("DatabaseManager is not initialized yet.")

    private var inited = false
    internal fun init() {
        if (inited) return

        _mainDatabase = Database.connect(mainDataSource, {
            SQLiteConfig().apply {
                setJournalMode(SQLiteConfig.JournalMode.WAL)
                setSynchronous(SQLiteConfig.SynchronousMode.NORMAL)

                apply(it)
            }
        })
        TransactionManager.defaultDatabase = _mainDatabase

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.create(MessageHistoryTable)
        }

        inited = true
    }

    fun makeBackup() {
        logger.info { "Backup database..." }
        runCatching {
            Path("data/sqlite.db").copyTo(Path("data/backup_sqlite.db"), overwrite = true)
            logger.info { "Backup database completed." }
        }.onFailure {
            if (it is NoSuchFileException) {
                return
            }

            throw it
        }
    }

    private val mainDatabaseMutex = Mutex()

    suspend fun <R> withMainDatabaseLock(block: suspend () -> R) = mainDatabaseMutex.withLock {
        block()
    }
}

suspend inline fun <R> mainTransaction(noinline block: JdbcTransaction.() -> R) = DatabaseManager.withMainDatabaseLock {
    transaction(DatabaseManager.mainDatabase, block)
}