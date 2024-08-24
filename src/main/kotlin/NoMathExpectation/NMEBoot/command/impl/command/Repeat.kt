package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.command.source.reply
import NoMathExpectation.NMEBoot.command.util.requiresPermission

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandRepeat() =
    literal("repeat")
        .requiresPermission("command.common.repeat")
        .collectGreedyString("text")
        .executes {
            val str = getString("text") ?: " "
            it.reply(str)
        }