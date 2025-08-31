package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.util.Canvas
import love.forte.simbot.ability.ReplySupport

suspend fun <S> LiteralSelectionCommandNode<S>.commandDraw()
        where S : PermissionAware, S : ReplySupport = literal("draw", "d")
    .requiresPermission("command.common.draw")
    .collectGreedyString("inst")
    .executes {
        val inst = getString("inst") ?: error("未提供绘图指令。")
        if (inst.trim().lowercase() in listOf("?", "help")) {
            it.reply(Canvas.INST_PATTERN_INFO)
            return@executes
        }

        runCatching {
            val canvas = Canvas.createFromInstructions(inst)
            val image = canvas.use {
                it.exportToImage()
            }

            it.reply(image)
        }.onFailure { e ->
            it.reply("绘图失败：${e.message}")
        }
    }