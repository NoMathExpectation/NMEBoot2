package NoMathExpectation.NMEBoot.message.onebot.apiExt

import NoMathExpectation.NMEBoot.message.onebot.LagrangeForwardNode
import NoMathExpectation.NMEBoot.message.onebot.wrap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult

class LagrangeSendGroupForwardMsg(
    override val body: Body
) : OneBotApi<LagrangeSendGroupForwardMsg.Result> {
    override val action = ACTION
    override val resultDeserializer = Result.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "send_group_forward_msg"
        val RES_SER = OneBotApiResult.serializer(Result.serializer())

        fun create(groupId: LongID, messages: List<LagrangeForwardNode>) = LagrangeSendGroupForwardMsg(
            Body(
                groupId,
                messages.wrap(),
            )
        )
    }

    @Serializable
    data class Body(
        @SerialName("group_id")
        val groupId: LongID,
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