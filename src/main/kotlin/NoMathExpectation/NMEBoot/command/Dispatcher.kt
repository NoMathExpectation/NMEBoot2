package NoMathExpectation.NMEBoot.command

import NoMathExpectation.NMEBoot.command.parser.CommandDispatcher
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.command.source.CommandSource
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

val commandDispatcher = CommandDispatcher<CommandSource<*>> {
    literal("stop").executes {
        it.reply("Stopping!")
    }

    literal("repeat").executes {
        reader.alignNextWord()
        val str = reader.readRemain() ?: " "
        it.reply(str)
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