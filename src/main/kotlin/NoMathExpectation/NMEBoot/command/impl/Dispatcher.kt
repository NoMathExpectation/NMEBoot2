package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.impl.command.*
import NoMathExpectation.NMEBoot.command.impl.command.admin.commandCooldown
import NoMathExpectation.NMEBoot.command.impl.command.admin.commandPermission
import NoMathExpectation.NMEBoot.command.impl.command.admin.commandStop
import NoMathExpectation.NMEBoot.command.impl.command.common.*
import NoMathExpectation.NMEBoot.command.impl.command.rd.commandChart
import NoMathExpectation.NMEBoot.command.impl.command.rd.commandConvert
import NoMathExpectation.NMEBoot.command.impl.command.rd.commandOffset
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.CommandDispatcher
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.SelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.literals
import NoMathExpectation.NMEBoot.command.parser.node.on
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

lateinit var commandDispatcher: CommandDispatcher<AnyExecuteContext>
    private set

private fun InsertableCommandNode<AnyExecuteContext>.onCommandPrefix() = on { ctx ->
    val prefix = commandConfig.get().commandPrefix
    (!ctx.requiresCommandPrefix || reader.peekString(prefix.length) == prefix).also { if (it) reader.next += prefix.length }
}

suspend fun initDispatcher() {
    logger.info { "构建指令树......" }

    commandDispatcher = CommandDispatcher(SelectionCommandNode()) {
        onCommandPrefix()
            .consumeCooldown()
            .literals {
                commandStop()
                commandRepeat()
                commandLuck()
                commandWhoAmI()
                commandPermission()
                commandTransfer()
                commandHelp()
                commandCooldown()
                commandEat()

                //rd
                commandChart()
                commandConvert()
                commandOffset()

                //experimental
                commandExport()
                commandRef()
                commandFiles()
                commandCopy()
            }

        commandEatShortcut()
    }
}

suspend fun <T> CommandSource<T>.executeCommand(
    command: String,
    contextBlock: ExecuteContext.Builder<T, T, T>.() -> Unit = {},
) {
    if (!hasPermission("use.command")) {
        return
    }

    val executeContext = ExecuteContext(this, contextBlock)
    val result = commandDispatcher.dispatch(executeContext, command)

    val exceptions = result.exceptions
    if (exceptions.isNotEmpty()) {
        exceptions.forEach {
            logger.error(it) { "Error while executing $command from $this: " }
        }

        val debug = hasPermission("use.debug")

        val firstDisplayException = if (debug) exceptions.firstOrNull() else exceptions.firstOrNull { it.showToUser }
        val firstDisplayMsg = firstDisplayException?.message ?: "未知错误"
        val msg = when {
            debug && exceptions.size > 1 -> "运行指令时产生了${exceptions.size}个错误，第一个错误为 $firstDisplayMsg"
            debug -> firstDisplayMsg
            firstDisplayException != null -> firstDisplayMsg
            else -> "工口发生。"
        }
        reply(msg)
    }
}