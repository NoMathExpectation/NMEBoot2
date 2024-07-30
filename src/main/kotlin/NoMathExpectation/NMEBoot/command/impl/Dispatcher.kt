package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.impl.command.commandLuck
import NoMathExpectation.NMEBoot.command.impl.command.commandRepeat
import NoMathExpectation.NMEBoot.command.impl.command.commandStop
import NoMathExpectation.NMEBoot.command.parser.CommandDispatcher
import NoMathExpectation.NMEBoot.command.source.CommandSource
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

lateinit var commandDispatcher: CommandDispatcher<CommandSource<*>>
    private set

suspend fun initDispatcher() {
    logger.info { "构建指令树......" }

    commandDispatcher = CommandDispatcher {
        commandStop()
        commandRepeat()
        commandLuck()
    }
}

suspend fun CommandSource<*>.executeCommand(command: String) {
    val result = commandDispatcher.dispatch(this, command)

    if (result.executeExceptions.isNotEmpty()) {
        reply("运行指令时产生了${result.executeExceptions.size}个错误。")
        result.executeExceptions.forEach {
            logger.error(it) { "Error while executing $command from $this: " }
        }
    }
}