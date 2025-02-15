package NoMathExpectation.NMEBoot.testing.command.source

import NoMathExpectation.NMEBoot.command.impl.executeCommand
import NoMathExpectation.NMEBoot.command.impl.initDispatcher
import NoMathExpectation.NMEBoot.util.asMessages
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import love.forte.simbot.message.PlainText
import kotlin.test.*

class CommandSourceTest {
    @BeforeTest
    fun init() = runBlocking {
        initDispatcher()
    }

    @Test
    fun testPermission() = runTest {
        val source = FakeCommandSource()
        source.setPermission("use.command", true)
        source.setPermission("command.common", true)

        source.executeCommand("//luck")

        assertEquals(1, source.receivedReplyMessage.size)

        val msg = source.receivedReplyMessage.first().asMessages().first()
        assertIs<PlainText>(msg)
        assertTrue {
            msg.text.startsWith("你今天的运气是: ")
        }
    }
}