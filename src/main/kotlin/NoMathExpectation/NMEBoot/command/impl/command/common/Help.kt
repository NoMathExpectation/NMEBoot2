package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.commandDispatcher
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.argument.optionallyCollectGreedyString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandHelp() =
    literal("help")
        .requiresPermission("command.common.help")
        .optionallyCollectGreedyString("command")
        .executes("获取指令帮助") {
            val command = getString("command") ?: ""
            it.reply(buildString {
                if (command.isNotBlank()) {
                    appendLine("$command...")
                }
                appendLine(commandDispatcher.completion(it, command) ?: "没有可用的帮助")
            })
        }