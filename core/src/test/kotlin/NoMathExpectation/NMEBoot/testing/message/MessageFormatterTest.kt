package NoMathExpectation.NMEBoot.testing.message

import NoMathExpectation.NMEBoot.message.deserializeToMessage
import NoMathExpectation.NMEBoot.util.startKoinApplication
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import love.forte.simbot.common.id.IntID.Companion.ID
import love.forte.simbot.message.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageFormatterTest {
    @BeforeTest
    fun init() = runBlocking {
        startKoinApplication()
    }

    @Test
    fun test() = runTest {
        val string =
            "123[at:user:456]789[atAll][image:id:123][ref:456]789[emoji:123][face:456]\\]789\\[123[unknown[]element\\[\\]type]456[]"
        val expected = buildMessages {
            +"123"
            +At(456.ID)
            +"789"
            +AtAll
            +RemoteIDImage(123.ID)
            +MessageIdReference(456.ID)
            +"789"
            +Emoji(123.ID)
            +Face(456.ID)
            +"]789[123"
            +""
            +"456"
            +""
        }

        val actual = string.deserializeToMessage()
        assertEquals(expected, actual)
    }
}