package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.impl.source.reply
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.stopProgram

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandStop() =
    literal("stop")
        .requiresPermission("command.admin.stop")
        .executes {
            it.reply("Stopping!")
            stopProgram()
        }