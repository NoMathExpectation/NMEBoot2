package NoMathExpectation.NMEBoot.testing.message

import NoMathExpectation.NMEBoot.message.onebot.apiExt.napcat.toNapcat
import kotlinx.serialization.encodeToString
import love.forte.simbot.common.id.IntID.Companion.ID
import love.forte.simbot.component.onebot.v11.core.OneBot11
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode
import love.forte.simbot.component.onebot.v11.message.segment.OneBotText
import kotlin.test.Test

class ForwardMessageTest {
    @Test
    fun test() {
        val node = OneBotForwardNode.create(
            1.ID,
            "test",
            listOf(OneBotText.create("111"))
        )
        val wrapper = node.toNapcat()
        println(OneBot11.DefaultJson.encodeToString(wrapper))
    }
}