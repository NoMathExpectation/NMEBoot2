package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.impl.source.reply
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandHelp() =
    literal("help")
        .requiresPermission("command.common.help")
        .executes {
            it.reply("没做。")
        }