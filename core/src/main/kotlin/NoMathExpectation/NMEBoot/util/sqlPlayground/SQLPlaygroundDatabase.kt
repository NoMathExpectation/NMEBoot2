package NoMathExpectation.NMEBoot.util.sqlPlayground

import NoMathExpectation.NMEBoot.util.toReadableText
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

val playgroundDatabaseConfig = HikariConfig().apply {
    jdbcUrl = "jdbc:postgresql://sql-playground:5432/playground"
    driverClassName = "org.postgresql.Driver"
    username = "playground"
    password = "playground"
}

val playgroundDataSource = HikariDataSource(playgroundDatabaseConfig)

val playgroundDatabaseConnection = Database.connect(playgroundDataSource)

fun executeSQLInPlaygroundDatabase(sql: String): String? {
    return transaction(playgroundDatabaseConnection) {
        exec(sql) { it.toReadableText() }
    }
}