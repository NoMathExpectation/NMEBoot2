package NoMathExpectation.NMEBoot.message.element

import NoMathExpectation.NMEBoot.message.onebot.apiExt.LagrangeGetGroupFileUrl
import NoMathExpectation.NMEBoot.util.asMessages
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import love.forte.simbot.annotations.ExperimentalSimbotAPI
import love.forte.simbot.common.id.LongID
import love.forte.simbot.component.kook.message.KookCardMessage
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.event.notice.RawGroupUploadEvent
import love.forte.simbot.kook.objects.card.CardModule
import love.forte.simbot.message.Message
import java.io.InputStream

interface Attachment : Message.Element {
    val name: String

    suspend fun inputStream(): InputStream
}

private val downloadClient = HttpClient {

}

class OneBotIncomingAttachment(
    val bot: OneBotBot,
    val groupId: LongID,
    val fileInfo: RawGroupUploadEvent.FileInfo,
) : Attachment {
    override val name = fileInfo.name

    override suspend fun inputStream(): InputStream {
        val api = LagrangeGetGroupFileUrl.create(groupId, fileInfo)
        val result = bot.executeResult(api)
        val url = result.data?.url ?: error("获取文件链接失败")

        val response = downloadClient.get(url)
        if (!response.status.isSuccess()) {
            error("下载文件失败")
        }
        return response.bodyAsChannel().toInputStream()
    }
}

class KookIncomingAttachment(
    val card: CardModule.Files
) : Attachment {
    override val name = card.title

    override suspend fun inputStream(): InputStream {
        val response = downloadClient.get(card.src)
        if (!response.status.isSuccess()) {
            error("下载文件失败")
        }
        return response.bodyAsChannel().toInputStream()
    }
}

fun CardModule.Files.asAttachment() = KookIncomingAttachment(this)

@OptIn(ExperimentalSimbotAPI::class)
fun Message.findKookAttachment() =
    asMessages()
        .filterIsInstance<KookCardMessage>()
        .flatMap { it.cards }
        .flatMap { it.modules }
        .filterIsInstance<CardModule.Files>()
        .map { it.asAttachment() }