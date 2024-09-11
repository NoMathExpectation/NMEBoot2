package NoMathExpectation.NMEBoot.command.impl.command.rd

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.ext.collectAttachment
import NoMathExpectation.NMEBoot.command.parser.argument.ext.getAttachments
import NoMathExpectation.NMEBoot.command.parser.argument.getBoolean
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.argument.optionallyCollectBoolean
import NoMathExpectation.NMEBoot.command.parser.argument.optionallyCollectString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.message.element.saveAsTempFile
import NoMathExpectation.NMEBoot.message.element.toAttachment
import NoMathExpectation.NMEBoot.util.BpmOffsetAnalyzer
import NoMathExpectation.NMEBoot.util.storageOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.UUID
import love.forte.simbot.message.buildMessages
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger { }

@Serializable
private data class Config(
    val ffmpeg: String = "ffmpeg",
    val timeout: Long = 60L,
)

private val configStorage = storageOf("config/convert.json", Config())

private suspend fun File.tryConvertTo(type: String, video: Boolean): File = withContext(Dispatchers.IO) {
    val config = configStorage.get()

    File("data/temp").mkdirs()
    val file = Path("data", "temp", "${UUID.random()}.$type").toFile()
    file.deleteOnExit()

    val process = if (video) {
        Runtime.getRuntime().exec(arrayOf(config.ffmpeg, "-xerror", "-nostdin", "-y", "-i", path, file.path))
    } else {
        Runtime.getRuntime().exec(arrayOf(config.ffmpeg, "-xerror", "-nostdin", "-vn", "-y", "-i", path, file.path))
    }

    if (!process.waitFor(config.timeout, TimeUnit.SECONDS)) {
        process.destroy()
        file.delete()
        throw RuntimeException("转换超时")
    }

    val error = process.errorStream
        .reader()
        .use { it.readText() }
        .lineSequence()
        .lastOrNull() ?: "未知错误"
    if (file.length() <= 0) {
        file.delete()
        throw RuntimeException(error)
    }

    file
}

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandConvert() =
    literal("convert")
        .requiresPermission("command.rd.fanmade.convert")
        .collectAttachment("files")
        .optionallyCollectBoolean("analyzeBpmAndOffset")
        .optionallyCollectString("type")
        .optionallyCollectBoolean("video")
        .executes {
            val attachment = getAttachments("files")?.firstOrNull() ?: run {
                it.reply("未找到文件。")
                return@executes
            }
            val analyzeBpmAndOffset = getBoolean("analyzeBpmAndOffset") != false
            val type = getString("type") ?: "ogg"
            val video = getBoolean("video") == true

            runCatching {
                val newFile = attachment.saveAsTempFile().tryConvertTo(type, video)
                val newName = attachment.name.substringBeforeLast('.') + "." + type
                val newAttachment = newFile.toAttachment(newName)

                it.send(newAttachment)
                if (!analyzeBpmAndOffset) {
                    return@executes
                }

                val (bpm, adjustedBpm, offset) = BpmOffsetAnalyzer.getBpmAndOffset(newFile)
                it.reply(buildMessages {
                    +"bpm: %.3f (%.3f)\n".format(adjustedBpm, bpm)
                    +"偏移: ${(offset * 1000).roundToInt()}ms"
                })
            }.onFailure { e ->
                logger.error(e) { "转换失败" }
                it.reply("转换失败")
            }
        }