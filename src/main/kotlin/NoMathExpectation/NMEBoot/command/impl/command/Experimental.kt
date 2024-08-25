package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.command.source.reply
import NoMathExpectation.NMEBoot.command.util.requiresPermission
import NoMathExpectation.NMEBoot.util.TransferSh
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

private val logger = KotlinLogging.logger { }

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandTransfer() =
    literal("transfer")
        .requiresPermission("command.experimental.transfer")
        .collectGreedyString("text")
        .executes {
            val str = getString("text") ?: " "
            val inputStream = str.byteInputStream()
            val uuid = UUID.randomUUID().toString()
            val link = TransferSh.upload(
                uuid,
                inputStream,
            ) {
                maxDay = 1
            }

            it.reply(link)
        }

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandRef() =
    literal("ref")
        .requiresPermission("command.experimental.ref")
        .executes {
            logger.info { }
        }