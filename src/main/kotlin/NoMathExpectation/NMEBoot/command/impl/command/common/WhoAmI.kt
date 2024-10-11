package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandWhoAmI() =
    literal("whoami")
        .requiresPermission("command.common.whoami")
        .executes("我是谁") {
            it.reply(it.target.permissionIds.joinToString())
        }