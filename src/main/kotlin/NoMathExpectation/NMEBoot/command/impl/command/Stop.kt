package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.command.util.requirePermission
import NoMathExpectation.NMEBoot.stopProgram

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandStop() =
    literal("stop")
        .requirePermission("command.stop")
        .executes {
            it.reply("Stopping!")
            stopProgram()
        }