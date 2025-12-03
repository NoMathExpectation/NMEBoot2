package NoMathExpectation.NMEBoot.database

import NoMathExpectation.NMEBoot.database.message.MessageHistoryTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

object DatabaseManager {
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

        _mainDatabase = Database.connect(mainDataSource)
        TransactionManager.defaultDatabase = _mainDatabase

        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction {
            SchemaUtils.create(MessageHistoryTable)
        }

        inited = true
    }
}