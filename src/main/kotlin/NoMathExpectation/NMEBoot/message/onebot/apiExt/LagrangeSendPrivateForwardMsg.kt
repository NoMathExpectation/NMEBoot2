package NoMathExpectation.NMEBoot.message.onebot.apiExt

import NoMathExpectation.NMEBoot.message.onebot.LagrangeForwardNode
import NoMathExpectation.NMEBoot.message.onebot.wrap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult

class LagrangeSendPrivateForwardMsg(
    override val body: Body
) : OneBotApi<LagrangeSendPrivateForwardMsg.Result> {
    override val action = ACTION
    override val resultDeserializer = Result.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "send_private_forward_msg"
        val RES_SER = OneBotApiResult.serializer(Result.serializer())

        fun create(userId: LongID, messages: List<LagrangeForwardNode>) = LagrangeSendPrivateForwardMsg(
            Body(
                userId,
                messages.wrap(),
            )
        )
    }

    @Serializable
    data class Body(
        @SerialName("user_id")
        val userId: LongID,
        val messages: List<LagrangeForwardNode.Wrapper> = listOf()
    )

    @Serializable
    data class Result(
        @SerialName("message_id")
        val messageId: LongID,
        @SerialName("forward_id")
        val forwardId: StringID,
    )
}