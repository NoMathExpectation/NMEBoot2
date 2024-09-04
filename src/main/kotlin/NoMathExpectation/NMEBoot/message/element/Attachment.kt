package NoMathExpectation.NMEBoot.message.element

import NoMathExpectation.NMEBoot.message.onebot.apiExt.LagrangeDeleteGroupFile
import NoMathExpectation.NMEBoot.message.onebot.apiExt.LagrangeGetGroupFileUrl
import NoMathExpectation.NMEBoot.util.asMessages
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import love.forte.simbot.ability.DeleteOption
import love.forte.simbot.ability.DeleteSupport
import love.forte.simbot.ability.StandardDeleteOption
import love.forte.simbot.annotations.ExperimentalSimbotAPI
import love.forte.simbot.common.id.LongID
import love.forte.simbot.component.kook.message.KookCardMessage
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.event.notice.RawGroupUploadEvent
import love.forte.simbot.kook.objects.card.CardModule
import love.forte.simbot.message.Message
import java.io.File
import java.io.InputStream

interface Attachment : Message.Element, DeleteSupport {
    val name: String

    suspend fun inputStream(): InputStream
}

suspend fun Attachment.deleteAfterDelay(delay: Long) = coroutineScope {
    launch {
        delay(delay)
        withContext(NonCancellable) {
            delete(
                StandardDeleteOption.IGNORE_ON_NO_SUCH_TARGET,
                StandardDeleteOption.IGNORE_ON_FAILURE,
                StandardDeleteOption.IGNORE_ON_UNSUPPORTED,
            )
        }
    }
}

private val downloadClient = HttpClient {

}

class OneBotIncomingAttachment(
    val bot: OneBotBot,
    val groupId: LongID,
    val fileInfo: RawGroupUploadEvent.FileInfo,
) : Attachment {
    override val name = fileInfo.name

    override suspend fun inputStream() = withContext(Dispatchers.IO) {
        val api = LagrangeGetGroupFileUrl.create(groupId, fileInfo)
        val result = bot.executeResult(api)
        val url = result.data?.url ?: error("获取文件链接失败")

        val response = downloadClient.get(url)
        if (!response.status.isSuccess()) {
            error("下载文件失败")
        }
        response.bodyAsChannel().toInputStream()
    }

    override suspend fun delete(vararg options: DeleteOption) {
        val api = LagrangeDeleteGroupFile.create(groupId, fileInfo)
        runCatching {
            bot.executeResult(api).dataOrThrow
        }.onFailure {
            if (StandardDeleteOption.IGNORE_ON_FAILURE !in options) {
                throw it
            }
        }
    }

    override fun toString() = "OneBotIncomingAttachment(name=$name)"
}

class KookIncomingAttachment(
    val card: CardModule.Files
) : Attachment {
    override val name = card.title

    override suspend fun inputStream() = withContext(Dispatchers.IO) {
        val response = downloadClient.get(card.src)
        if (!response.status.isSuccess()) {
            error("下载文件失败")
        }
        response.bodyAsChannel().toInputStream()
    }

    override suspend fun delete(vararg options: DeleteOption) {
        if (StandardDeleteOption.IGNORE_ON_UNSUPPORTED !in options) {
            throw UnsupportedOperationException("KookIncomingAttachment不支持删除")
        }
    }

    override fun toString() = "KookIncomingAttachment(name=$name)"
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

class InputStreamAttachment(
    override val name: String,
    private val stream: InputStream,
) : Attachment {
    override suspend fun inputStream() = stream

    override suspend fun delete(vararg options: DeleteOption) {
        if (StandardDeleteOption.IGNORE_ON_UNSUPPORTED !in options) {
            throw UnsupportedOperationException("InputStreamAttachment不支持删除")
        }
    }

    override fun toString() = "InputStreamAttachment(name=$name)"
}

class FileAttachment(
    val file: File
) : Attachment {
    override val name = file.name

    override suspend fun inputStream() = withContext(Dispatchers.IO) {
        file.inputStream()
    }

    override suspend fun delete(vararg options: DeleteOption) {
        if (StandardDeleteOption.IGNORE_ON_UNSUPPORTED !in options) {
            throw UnsupportedOperationException("InputStreamAttachment不支持删除")
        }
    }

    override fun toString() = "FileAttachment(name=$name)"
}