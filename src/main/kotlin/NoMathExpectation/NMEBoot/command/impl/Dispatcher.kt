package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.impl.command.*
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.CommandDispatcher
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

lateinit var commandDispatcher: CommandDispatcher<AnyExecuteContext>
    private set

suspend fun initDispatcher() {
    logger.info { "构建指令树......" }

    commandDispatcher = CommandDispatcher {
        commandStop()
        commandRepeat()
        commandLuck()
        commandWhoAmI()
        commandChart()
        commandPermission()
        commandTransfer()
        commandHelp()

        //experimental
        commandExport()
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

    if (result.executeExceptions.isNotEmpty()) {
        val e = result.executeExceptions.first()
        val msg = e.localizedMessage ?: e.message ?: "未知错误"

        if (result.executeExceptions.size == 1) {
            logger.error(e) { "Error while executing $command from $this: " }
            reply(msg)
            return
        }

        result.executeExceptions.forEach {
            logger.error(it) { "Error while executing $command from $this: " }
        }
        reply("运行指令时产生了${result.executeExceptions.size}个错误，第一个错误为 $msg")

        return
    }

    if (result.parseExceptions.isNotEmpty()) {
        val e = result.parseExceptions.first()
        val msg = e.localizedMessage ?: e.message ?: "未知错误"

        reply(msg)
        return
    }
}