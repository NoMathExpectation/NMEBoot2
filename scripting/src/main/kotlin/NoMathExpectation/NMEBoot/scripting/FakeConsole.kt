package NoMathExpectation.NMEBoot.scripting

class FakeConsole {
    private val outputBuilder = StringBuilder()

    fun print(obj: Any?) {
        outputBuilder.append(obj)
    }

    fun println(obj: Any?) {
        outputBuilder.appendLine(obj)
    }

    fun readln(): String = error("EOF has already been reached")

    fun readLine(): String? = null

    fun readlnOrNull(): String? = null

    val output get() = outputBuilder.toString()
}