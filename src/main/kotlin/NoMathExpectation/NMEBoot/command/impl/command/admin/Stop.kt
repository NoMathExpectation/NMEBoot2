package NoMathExpectation.NMEBoot.command.impl.command.admin

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.stopProgram
import love.forte.simbot.ability.ReplySupport

suspend fun <T> LiteralSelectionCommandNode<T>.commandStop()
        where T : ReplySupport,
              T : PermissionAware =
    literal("stop")
        .requiresPermission("command.admin.stop")
        .executes("停止服务器") {
            it.reply("Stopping!")
            stopProgram()
        }