package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.command.util.requirePermission

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandWhoAmI() =
    literal("whoami")
        .requirePermission("command.whoami", true)
        .executes {
            it.reply(it.permissionIds.joinToString())
        }