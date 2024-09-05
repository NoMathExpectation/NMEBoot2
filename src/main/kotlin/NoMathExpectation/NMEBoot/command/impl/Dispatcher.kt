package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.impl.command.*
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.CommandDispatcher
import NoMathExpectation.NMEBoot.util.TimeRefreshable
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
        commandPermission()
        commandTransfer()
        commandHelp()
        commandCooldown()

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
}

suspend fun <T> CommandSource<T>.executeCommand(
    command: String,
    contextBlock: ExecuteContext.Builder<T, T, T>.() -> Unit = {},
) {
    if (!hasPermission("use.command")) {
        return
    }

    if (!hasPermission("bypass.cooldown")) {
        val msg = CooldownConfig.storage.referenceUpdate {
            val timer = globalSubjectPermissionId?.let { id ->
                it.getGroupUser(id, uid)
            } ?: it.getPrivateUser(uid)

            if (timer.use()) {
                return@referenceUpdate null
            }

            if (timer is TimeRefreshable) {
                "请等待${timer.timeUntilRefresh.inWholeSeconds}s后使用指令"
            } else {
                "指令使用次数已用完"
            }
        }

        msg?.let {
            reply(it)
            return
        }
    }

    val executeContext = ExecuteContext(this, contextBlock)
    val result = commandDispatcher.dispatch(executeContext, command)

    if (result.executeExceptions.isNotEmpty()) {
        result.executeExceptions.forEach {
            logger.error(it) { "Error while executing $command from $this: " }
        }

        val debug = hasPermission("use.debug")
        val e = result.executeExceptions.first()
        val firstErrorMsg = if (debug) {
            e.message ?: "未知错误"
        } else {
            "工口发生。"
        }

        val errors = result.executeExceptions.size
        if (errors == 1) {
            reply(firstErrorMsg)
            return
        }
        val msg = if (debug) {
            "运行指令时产生了${errors}个错误，第一个错误为 $firstErrorMsg"
        } else {
            "${errors}次工口发生。"
        }
        reply(msg)

        return
    }

    if (result.parseExceptions.isNotEmpty()) {
        val e = result.parseExceptions.first()
        val msg = e.localizedMessage ?: e.message ?: "未知错误"

        reply(msg)
        return
    }
}