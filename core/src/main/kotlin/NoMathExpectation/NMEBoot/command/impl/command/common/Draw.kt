package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.getBoolean
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.message.unescapeMessageFormatIdentifiers
import NoMathExpectation.NMEBoot.util.canvas.Canvas
import NoMathExpectation.NMEBoot.util.canvas.InstructionReader
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.ability.ReplySupport
import love.forte.simbot.message.buildMessages

private val logger = KotlinLogging.logger { }

suspend fun <S> LiteralSelectionCommandNode<S>.commandDraw()
        where S : PermissionAware, S : ReplySupport = literal("draw", "d")
    .requiresPermission("command.common.draw")
    .select {
        blockOptions = true
        help = "绘制图像"

        val executeNode = ForwardCommandNode<S>()

        literals {
            literal("help", "?")
                .executes("查看指令帮助") {
                    it.reply(InstructionReader.INST_PATTERN_INFO)
                }

            literal("verbose", "-v")
                .executes("输出更多信息") {
                    set("verbose", true)
                }.forward(executeNode, "输出更多信息")
        }

        val collect = collectGreedyString("inst")
        executeNode.next = collect
        collect.executes("绘制图像") {
            val inst = getString("inst")?.unescapeMessageFormatIdentifiers() ?: error("未提供绘图指令。")
            val verbose = getBoolean("verbose") ?: false

            runCatching {
                val (canvas, reader) = Canvas.createFromInstructions(inst)
                val image = canvas.use { canvas ->
                    canvas.exportToImage()
                }

                it.reply(buildMessages {
                    +image
                    +"\n"
                    if (verbose) {
                        +reader.variablesToString()
                    }
                })
            }.onFailure { e ->
                logger.error(e) { "绘制图像时发生了一个异常：" }
                it.reply("绘图失败：\n${e.message}")
            }
        }
    }