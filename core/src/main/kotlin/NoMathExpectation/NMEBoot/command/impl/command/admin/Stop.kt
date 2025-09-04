package NoMathExpectation.NMEBoot.command.impl.command.admin

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.ability.ReplySupport
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger { }

suspend fun <T> LiteralSelectionCommandNode<T>.commandStop()
        where T : ReplySupport,
              T : PermissionAware =
    literal("stop")
        .requiresPermission("command.admin.stop")
        .executes("停止服务器") {
            logger.info { "Stopping the program..." }
            it.reply("Stopping!")
            //stopProgram() // it just don't work.
            exitProcess(0)
        }