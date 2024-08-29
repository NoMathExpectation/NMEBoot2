package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import love.forte.simbot.ability.ReplySupport

suspend fun <T> LiteralSelectionCommandNode<T>.commandHelp()
        where T : ReplySupport,
              T : PermissionAware =
    literal("help")
        .requiresPermission("command.common.help")
        .executes {
            it.reply("没做。")
        }