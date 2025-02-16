package NoMathExpectation.NMEBoot.scripting.testing

import NoMathExpectation.NMEBoot.scripting.evalScript
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TestScript {
    @Test
    fun testRunning() {
        val str = "Hello, world!"
        val ret = 114514
        val script = """
            println("$str")
            $ret
        """.trimIndent()

        val result = script.evalScript()
        val reports = result.reports
        reports.map { it.severity to it.message }.forEach(::println)
        assertIs<ResultWithDiagnostics.Success<EvaluationResult>>(result, "Script failed to run")

        val returnValue = result.value.returnValue
        assertIs<ResultValue.Value>(returnValue)
        assertEquals(ret, returnValue.value)
    }
}