package NoMathExpectation.NMEBoot.scripting

import java.io.InputStream

class FakeConsole(val input: InputStream = InputStream.nullInputStream()) : AutoCloseable {
    private val inputReader = input.bufferedReader()

    fun readln(): String = readlnOrNull() ?: error("EOF has already been reached")

    fun readlnOrNull(): String? = readLine()

    fun readLine(): String? = inputReader.readLine()

    private val outputBuilder = StringBuilder()

    fun print(obj: Any?) {
        outputBuilder.append(obj)
    }

    fun println(obj: Any?) {
        outputBuilder.appendLine(obj)
    }

    val output get() = outputBuilder.toString()

    override fun close() {
        inputReader.close()
    }
}