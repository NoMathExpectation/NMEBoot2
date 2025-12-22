package NoMathExpectation.NMEBoot.database

import NoMathExpectation.NMEBoot.database.message.MessageHistoryTable
import NoMathExpectation.NMEBoot.util.storageOf
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection
import javax.sql.DataSource
import kotlin.io.path.Path
import kotlin.io.path.copyTo

object DatabaseManager {
    @Serializable
    private data class Config(
        val url: String = "r2dbc:postgresql://db:5432/nmeboot",
        val user: String? = "postgres",
        val password: String? = "password",
    )

    private val config = storageOf("config/database.json", Config())

    private val logger = KotlinLogging.logger { }

    fun createDataSource(jdbcUrl: String, user: String? = null, password: String? = null) =
        HikariDataSource(HikariConfig().apply {
        this.jdbcUrl = jdbcUrl
            user?.let { username = it }
            password?.let { this.password = it }
        transactionIsolation = "TRANSACTION_SERIALIZABLE"
        validate()
    })

    private var _mainDataSource: DataSource? = null
    val mainDataSource get() = _mainDataSource ?: error("DatabaseManager is not initialized yet.")
    private var _mainDatabase: Database? = null
    val mainDatabase get() = _mainDatabase ?: error("DatabaseManager is not initialized yet.")

    private var inited = false
    internal suspend fun init() {
        if (inited) return

        val config = config.get()

        _mainDataSource = createDataSource(config.url, config.user, config.password)
        _mainDatabase = Database.connect(mainDataSource)
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