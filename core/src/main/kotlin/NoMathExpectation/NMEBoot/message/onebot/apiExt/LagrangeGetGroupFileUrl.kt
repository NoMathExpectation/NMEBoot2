package NoMathExpectation.NMEBoot.message.onebot.apiExt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult
import love.forte.simbot.component.onebot.v11.event.notice.RawGroupUploadEvent

class LagrangeGetGroupFileUrl(
    override val body: Body
) : OneBotApi<LagrangeGetGroupFileUrl.Result> {
    override val action = ACTION
    override val resultDeserializer = Result.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "get_group_file_url"
        val RES_SER = OneBotApiResult.serializer(Result.serializer())

        fun create(groupId: LongID, fileInfo: RawGroupUploadEvent.FileInfo) = LagrangeGetGroupFileUrl(
            Body(
                groupId,
                fileInfo.id.toString().ID,
                fileInfo.busid,
            )
        )
    }

    @Serializable
    data class Body(
        @SerialName("group_id")
        val groupId: LongID,
        @SerialName("file_id")
        val fileId: StringID,
        @SerialName("busid")
        val busId: Long,
    )

    @Serializable
    data class Result(
        val url: String,
    )
}