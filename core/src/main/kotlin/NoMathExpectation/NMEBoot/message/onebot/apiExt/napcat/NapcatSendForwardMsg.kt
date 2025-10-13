package NoMathExpectation.NMEBoot.message.onebot.apiExt.napcat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode

class NapcatSendForwardMsg(
    override val body: Body
) : OneBotApi<NapcatSendForwardMsg.Result> {
    override val action = ACTION
    override val resultDeserializer = Result.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "send_forward_msg"
        val RES_SER = OneBotApiResult.serializer(Result.serializer())

        fun createGroup(groupID: StringID, messages: List<OneBotForwardNode>) = NapcatSendForwardMsg(
            Body(
                groupID,
                null,
                messages.map { it.toNapcat() },
            )
        )

        fun createPrivate(userID: StringID, messages: List<OneBotForwardNode>) = NapcatSendForwardMsg(
            Body(
                null,
                userID,
                messages.map { it.toNapcat() },
            )
        )
    }

    @Serializable
    data class Body(
        @SerialName("group_id")
        val groupId: StringID?,
        @SerialName("user_id")
        val userId: StringID?,
        val messages: List<NapcatForwardNode.Wrapper> = listOf()
    )

    @Serializable
    data class Result(
        @SerialName("message_id")
        val messageId: LongID,
        @SerialName("res_id")
        val resId: StringID,
    )
}