package NoMathExpectation.NMEBoot.message.onebot.apiExt.napcat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult
import love.forte.simbot.component.onebot.v11.event.notice.RawGroupUploadEvent

class NapcatGetGroupFileUrl(
    override val body: Body
) : OneBotApi<NapcatGetGroupFileUrl.Result> {
    override val action = ACTION
    override val resultDeserializer = Result.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "get_group_file_url"
        val RES_SER = OneBotApiResult.serializer(Result.serializer())

        fun create(groupId: LongID, fileInfo: RawGroupUploadEvent.FileInfo) = NapcatGetGroupFileUrl(
            Body(
                groupId,
                fileInfo.id.toString().ID,
            )
        )
    }

    @Serializable
    data class Body(
        @SerialName("group_id")
        val groupId: LongID,
        @SerialName("file_id")
        val fileId: StringID,
    )

    @Serializable
    data class Result(
        val url: String,
    )
}