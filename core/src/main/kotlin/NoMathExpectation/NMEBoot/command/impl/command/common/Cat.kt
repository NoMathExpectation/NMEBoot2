package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import love.forte.simbot.message.OfflineURIImage.Companion.toOfflineImage
import java.net.URI

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandCat() =
    literal("cat")
        .requiresPermission("command.common.cat")
        .executes("看看猫") {
            it.reply(URI("https://cataas.com/cat").toOfflineImage())
        }