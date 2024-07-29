package NoMathExpectation.NMEBoot

import NoMathExpectation.NMEBoot.bot.startSimbot
import NoMathExpectation.NMEBoot.bot.stopSimbot
import NoMathExpectation.NMEBoot.command.commandDispatcher
import NoMathExpectation.NMEBoot.command.executeCommand
import NoMathExpectation.NMEBoot.command.source.ConsoleCommandSource
import com.varabyte.kotter.foundation.firstSuccess
import com.varabyte.kotter.foundation.input.input
import com.varabyte.kotter.foundation.input.onInputEntered
import com.varabyte.kotter.foundation.input.runUntilInputEntered
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.text.rgb
import com.varabyte.kotter.foundation.text.text
import com.varabyte.kotter.terminal.system.SystemTerminal
import com.varabyte.kotter.terminal.virtual.VirtualTerminal

suspend fun main() {
    println("Hello World!")

    startSimbot()

    runCatching {
        consoleRoutine()
    }

    stopSimbot()
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

        while (true) {
            section {
                text("> ")
                rgb(inputColor) {
                    input()
                }
            }.runUntilInputEntered {
                onInputEntered {
                    command = input
                }
            }

            section {

            }.run {
                ConsoleCommandSource.executeCommand(command)
            }

            if (command == "stop") {
                break
            }
        }
    }
}