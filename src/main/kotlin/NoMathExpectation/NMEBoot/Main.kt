package NoMathExpectation.NMEBoot

import NoMathExpectation.NMEBoot.bot.startSimbot
import NoMathExpectation.NMEBoot.bot.stopSimbot
import NoMathExpectation.NMEBoot.command.impl.executeCommand
import NoMathExpectation.NMEBoot.command.impl.initDispatcher
import NoMathExpectation.NMEBoot.command.source.ConsoleCommandSource
import com.varabyte.kotter.foundation.firstSuccess
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.rgb
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.terminal.system.SystemTerminal
import com.varabyte.kotter.terminal.virtual.VirtualTerminal
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory

private val logger = KotlinLogging.logger { }

suspend fun main() {
    logger.info { "启动中......" }

    Path("config").createDirectories()
    Path("data").createDirectories()

    initDispatcher()

    startSimbot()

    runCatching {
        consoleRoutine()
    }

    stopSimbot()
}

private var stop = false
private var stopHandle: (() -> Boolean)? = null
private val stopMutex = Mutex()

suspend fun stopProgram() {
    stopMutex.withLock {
        stop = true
        stopHandle?.invoke()
    }
}

private const val inputColor = 0xb2b6b6

fun consoleRoutine() {
    session(
        terminal = listOf(
            { SystemTerminal() },
            { VirtualTerminal.create("NMEBoot Console") },
        ).firstSuccess()
    ) {
        var command = ""

        while (!stop) {
            section {
                text("> ")
                rgb(inputColor) {
                    input()
                }
            }.runUntilSignal {
                stopMutex.withLock {
                    if (stop) {
                        signal()
                        return@runUntilSignal
                    }
                    stopHandle = ::signal
                }

                onInputEntered {
                    command = input
                    signal()
                }
            }

            section {

            }.run {
                stopMutex.withLock {
                    stopHandle = null
                    if (stop) {
                        return@run
                    }
                }

                ConsoleCommandSource.executeCommand(command)
            }
        }
    }
}