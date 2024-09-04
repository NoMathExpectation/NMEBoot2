package NoMathExpectation.NMEBoot.message.onebot.apiExt

import NoMathExpectation.NMEBoot.message.onebot.LagrangeForwardNode
import NoMathExpectation.NMEBoot.message.onebot.wrap
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.StringID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult

class LagrangeSendForwardMsg(
    override val body: Body
) : OneBotApi<StringID> {
    override val action = ACTION
    override val resultDeserializer = StringID.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "send_forward_msg"
        val RES_SER = OneBotApiResult.serializer(StringID.serializer())

        fun create(messages: List<LagrangeForwardNode>) = LagrangeSendForwardMsg(
            Body(
                messages.wrap(),
            )
        )
    }

    @Serializable
    data class Body(
        val messages: List<LagrangeForwardNode.Wrapper> = listOf()
    )
}