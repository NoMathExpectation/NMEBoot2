package NoMathExpectation.NMEBoot.message.onebot.apiExt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApi
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult

class LagrangeUploadGroupFile(
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
        val file: StringID,
        val name: String,
        val folder: StringID? = null,
    )
}