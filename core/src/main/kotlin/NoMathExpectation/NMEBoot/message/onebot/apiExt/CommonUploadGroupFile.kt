package NoMathExpectation.NMEBoot.message.onebot.apiExt

import NoMathExpectation.NMEBoot.message.element.Attachment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.common.id.UUID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult
import java.io.File

class CommonUploadGroupFile(
    override val body: Body
) : OneBotApi<Unit> {
    override val action = ACTION
    override val resultDeserializer = Unit.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "upload_group_file"
        val RES_SER = OneBotApiResult.emptySerializer()
    }

    @Serializable
    data class Body(
        @SerialName("group_id")
        val groupId: LongID,
        val file: String,
        val name: String,
        val folder: StringID? = null,
    )
}

suspend fun Attachment.toOneBotGroupUploadApi(groupId: LongID, folder: StringID? = null): CommonUploadGroupFile {
    File("/swap/onebot/upload/").mkdirs()
    val fileName = "/swap/onebot/upload/${UUID.random()}"
    val file = File(fileName)

    file.outputStream().use { out ->
        inputStream().use {
            it.copyTo(out)
        }
    }
    file.deleteOnExit()

    return CommonUploadGroupFile(
        CommonUploadGroupFile.Body(
            groupId,
            fileName,
            name,
            folder,
        )
    )
}