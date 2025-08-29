package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import love.forte.simbot.ability.ReplySupport

suspend fun <T> LiteralSelectionCommandNode<T>.commandStatus()
        where T : PermissionAware, T : ReplySupport = literal("status")
    .requiresPermission("command.common.status")
    .executes("查询状态") {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val maxMemory = runtime.maxMemory()
        val usedMemory = totalMemory - runtime.freeMemory()

        val usedMemoryText = (usedMemory.toDouble() / 1024 / 1024 * 100).toInt() / 100.0
        val totalMemoryText = (totalMemory.toDouble() / 1024 / 1024 * 100).toInt() / 100.0
        val maxMemoryText = (maxMemory.toDouble() / 1024 / 1024 * 100).toInt() / 100.0

        it.reply("内存： $usedMemoryText MB / $totalMemoryText MB / $maxMemoryText MB")
    }