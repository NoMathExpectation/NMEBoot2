package NoMathExpectation.NMEBoot.message.onebot.apiExt

import NoMathExpectation.NMEBoot.message.element.Attachment
import NoMathExpectation.NMEBoot.message.onebot.apiExt.lagrange.OnebotLagrangeExtApi
import NoMathExpectation.NMEBoot.message.onebot.apiExt.napcat.OnebotNapcatExtApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.component.onebot.v11.core.api.OneBotApiResult
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.event.notice.RawGroupUploadEvent
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode
import love.forte.simbot.message.MessageReceipt

interface OnebotExtApi {
    val type: Type

    suspend fun deleteGroupFile(groupId: LongID, fileInfo: RawGroupUploadEvent.FileInfo)

    suspend fun getGroupFileUrl(groupId: LongID, fileInfo: RawGroupUploadEvent.FileInfo): String

    suspend fun sendGroupForwardMsg(groupId: LongID, messages: List<OneBotForwardNode>): MessageReceipt

    suspend fun sendPrivateForwardMsg(userId: LongID, messages: List<OneBotForwardNode>): MessageReceipt

    suspend fun uploadGroupFile(attachment: Attachment, groupId: LongID, folderId: StringID? = null)

    suspend fun uploadPrivateFile(attachment: Attachment, userId: LongID)

    companion object {
        private val botType = mutableMapOf<OneBotBot, Type>()
        private val botExtApiInstances = mutableMapOf<OneBotBot, OnebotExtApi>()

        fun registerBotType(bot: OneBotBot, type: Type) {
            botType[bot] = type
        }

        fun of(bot: OneBotBot) = botExtApiInstances.getOrPut(bot) {
            when (botType[bot]
                ?: error("Bot type not registered. Please register the bot type before using the extension API.")) {
                Type.LAGRANGE -> OnebotLagrangeExtApi(bot)
                Type.NAPCAT -> OnebotNapcatExtApi(bot)
            }
        }
    }

    @Serializable
    enum class Type {
        @SerialName("lagrange")
        LAGRANGE,

        @SerialName("napcat")
        NAPCAT,
    }
}

val OneBotBot.extApi get() = OnebotExtApi.of(this)

inline fun <T : Any> OneBotApiResult<T>.dataOrThrowCustom(messageBlock: OneBotApiResult<T>.() -> String) = runCatching {
    dataOrThrow
}.getOrElse {
    throw IllegalStateException(messageBlock(), it)
}