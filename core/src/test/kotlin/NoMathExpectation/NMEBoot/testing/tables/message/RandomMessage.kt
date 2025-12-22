package NoMathExpectation.NMEBoot.testing.tables.message

import NoMathExpectation.NMEBoot.database.DatabaseManager
import NoMathExpectation.NMEBoot.database.message.MessageHistory
import NoMathExpectation.NMEBoot.util.startKoinApplication
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.measureTime

class RandomMessage {
    init {
        startKoinApplication()
        runBlocking { DatabaseManager.init() }
    }

    @Test
    fun test() = runTest {
        val time = measureTime {
            println(MessageHistory.fetchRandomMessage("onebot", "foo", null))
        }
        println(time)
    }
}