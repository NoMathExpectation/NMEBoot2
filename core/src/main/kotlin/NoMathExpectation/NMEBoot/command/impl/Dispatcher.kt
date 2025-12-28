package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.impl.command.admin.commandCooldown
import NoMathExpectation.NMEBoot.command.impl.command.admin.commandPermission
import NoMathExpectation.NMEBoot.command.impl.command.admin.commandStop
import NoMathExpectation.NMEBoot.command.impl.command.commandTransfer
import NoMathExpectation.NMEBoot.command.impl.command.common.*
import NoMathExpectation.NMEBoot.command.impl.command.custom.bitterBirds
import NoMathExpectation.NMEBoot.command.impl.command.rd.*
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.CommandDispatcher
import NoMathExpectation.NMEBoot.command.parser.CommandExecuteException
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.node.SelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.literals
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.message.toText

private val logger = KotlinLogging.logger { }

lateinit var commandDispatcher: CommandDispatcher<AnyExecuteContext>
    private set

suspend fun initDispatcher() {
    logger.info { "构建指令树......" }

    commandDispatcher = CommandDispatcher(SelectionCommandNode()) {
        onCommandPrefix(commandConfig.get().commandPrefix)
            .consumeCooldown()
            .literals {
                commandStop()
                commandStatus()
                commandRepeat()
                commandLuck()
                commandRandom()
                commandWhoAmI()
                commandPermission()
                commandTransfer()
                commandHelp()
                commandCooldown()
                commandEat()
                commandEval()
                commandMCChat()
                commandDraw()
                commandSql()
                commandHistory()

                //rd
                commandChart()
                commandConvert()
                commandOffset()
                commandSamurai()

                //experimental
                //commandExport()
                //commandRef()
                //commandFiles()
                //commandCopy()
            }

        commandEatShortcut()

        linSunForCat()
        bitterBirds()
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

    var exceptions = result.exceptions
    exceptions.forEach {
        when (it) {
            is CommandParseException -> logger.debug(it) { "Command \"$command\" parse failed from $this: " }
            is CommandExecuteException -> logger.error(it) { "Error while executing \"$command\" from $this: " }
        }
    }

    exceptions = exceptions.filter { it is CommandExecuteException || it.showToUser }
    if (exceptions.isNotEmpty()) {
        val debug = isDebug()
        val canSamurai = canSamurai()
        val subjectPermissionId = subjectPermissionId

        val firstDisplayException = if (debug) exceptions.firstOrNull() else exceptions.firstOrNull { it.showToUser }
        val firstDisplayMsg = (firstDisplayException?.messageToUser ?: "未知错误").toText()
        val msg = when {
            debug && exceptions.size > 1 -> "运行指令时产生了${exceptions.size}个错误，第一个错误为 $firstDisplayMsg".toText()
            debug -> firstDisplayMsg
            canSamurai && exceptions.any { it is CommandExecuteException && !it.showToUser } && subjectPermissionId != null -> Samurai.exception(
                subjectPermissionId
            )
            firstDisplayException != null -> firstDisplayMsg
            else -> "工口发生。".toText()
        }
        reply(msg)
    }
}

suspend fun PermissionAware.isDebug() = hasPermission("use.debug")