package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandWhoAmI() =
    literal("whoami")
        .requiresPermission("command.common.whoami")
        .executes {
            it.reply(it.permissionIds.joinToString())
        }