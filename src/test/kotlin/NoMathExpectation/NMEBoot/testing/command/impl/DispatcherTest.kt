package NoMathExpectation.NMEBoot.testing.command.impl

import NoMathExpectation.NMEBoot.command.impl.ExecuteContext
import NoMathExpectation.NMEBoot.command.impl.commandDispatcher
import NoMathExpectation.NMEBoot.command.impl.initDispatcher
import NoMathExpectation.NMEBoot.command.impl.source.ConsoleCommandSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class DispatcherTest {
    val logger = KotlinLogging.logger { }

    @BeforeTest
    fun init() = runBlocking {
        initDispatcher()
    }

    @Test
    fun testHelp() = runTest {
        val completion = commandDispatcher.completion(ExecuteContext(ConsoleCommandSource), "")
        logger.info { completion }
    }
}