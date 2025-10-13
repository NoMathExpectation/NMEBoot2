package NoMathExpectation.NMEBoot.message.onebot.apiExt.lagrange

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode

class LagrangeSendGroupForwardMsg(
    override val body: Body
) : OneBotApi<LagrangeSendGroupForwardMsg.Result> {
    override val action = ACTION
    override val resultDeserializer = Result.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "send_group_forward_msg"
        val RES_SER = OneBotApiResult.serializer(Result.serializer())

        fun createFromLagrangeNodes(groupId: LongID, messages: List<LagrangeForwardNode>) = LagrangeSendGroupForwardMsg(
            Body(
                groupId,
                messages.wrap(),
            )
        )

        fun create(groupId: LongID, messages: List<OneBotForwardNode>) = LagrangeSendGroupForwardMsg(
            Body(
                groupId,
                messages.map { it.toLagrange() },
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