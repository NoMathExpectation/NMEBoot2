package NoMathExpectation.NMEBoot.message.onebot.apiExt

import NoMathExpectation.NMEBoot.message.element.Attachment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.UUID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult
import java.io.File

class CommonUploadPrivateFile(
    override val body: Body
) : OneBotApi<Unit> {
    override val action = ACTION
    override val resultDeserializer = Unit.serializer()
    override val apiResultDeserializer = RES_SER

    companion object {
        const val ACTION = "upload_private_file"
        val RES_SER = OneBotApiResult.emptySerializer()
    }

    @Serializable
    data class Body(
        @SerialName("user_id")
        val userId: LongID,
        val file: String,
        val name: String,
    )
}

suspend fun Attachment.toOneBotPrivateUploadApi(userId: LongID): CommonUploadPrivateFile {
    File("/swap/onebot/upload/").mkdirs()
    val fileName = "/swap/onebot/upload/${UUID.random()}"
    val file = File(fileName)

    file.outputStream().use { out ->
        inputStream().use {
            it.copyTo(out)
        }
    }
    file.deleteOnExit()

    return CommonUploadPrivateFile(
        CommonUploadPrivateFile.Body(
            userId,
            fileName,
            name,
        )
    )
}