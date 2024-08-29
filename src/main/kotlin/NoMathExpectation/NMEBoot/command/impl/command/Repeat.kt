package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import love.forte.simbot.ability.ReplySupport

suspend fun <T> LiteralSelectionCommandNode<T>.commandRepeat()
        where T : ReplySupport,
              T : PermissionAware =
    literal("repeat")
        .requiresPermission("command.common.repeat")
        .collectGreedyString("text")
        .executes {
            val str = getString("text") ?: " "
            it.reply(str)
        }