package NoMathExpectation.NMEBoot.command.impl.command.rd

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.ext.collectAttachment
import NoMathExpectation.NMEBoot.command.parser.argument.ext.getAttachments
import NoMathExpectation.NMEBoot.command.parser.argument.getDouble
import NoMathExpectation.NMEBoot.command.parser.argument.optionallyCollectDouble
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.message.element.saveAsTempFile
import NoMathExpectation.NMEBoot.util.BpmOffsetAnalyzer
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.message.buildMessages
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger { }

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandOffset() =
    literal("offset", "bpm")
        .requiresPermission("command.rd.fanmade.offset")
        .collectAttachment("files")
        .optionallyCollectDouble("knownBpm")
        .executes("测量bpm与偏移") {
            val attachment = getAttachments("files")?.firstOrNull() ?: run {
                it.reply("未找到文件。")
                return@executes
            }
            val knownBpm = getDouble("knownBpm")

            runCatching {
                val file = attachment.saveAsTempFile()
                val (bpm, adjustedBpm, offset) = BpmOffsetAnalyzer.getBpmAndOffset(file, knownBpm)
                it.reply(buildMessages {
                    +"bpm: %.3f (%.3f)\n".format(adjustedBpm, bpm)
                    +"偏移: ${(offset * 1000).roundToInt()}ms"
                })
            }.onFailure { e ->
                logger.error(e) { "分析offset失败" }
                it.reply("分析文件失败")
            }
        }