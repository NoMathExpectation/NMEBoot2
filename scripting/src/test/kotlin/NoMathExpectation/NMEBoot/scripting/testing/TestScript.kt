package NoMathExpectation.NMEBoot.scripting.testing

import NoMathExpectation.NMEBoot.scripting.toSimpleEval
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TestScript {
    @Test
    fun testRunning() {
        val input = "Hello, world!"
        val quoted = "1919810"
        val ret = 114514
        val script = """
            println(readln())
            print(quoted)
            $ret
        """.trimIndent()

        val simpleEval = script.toSimpleEval {
            this.input = input.byteInputStream()
            this.quoted = quoted
        }
        simpleEval()
        val result = simpleEval.result
        val reports = simpleEval.result!!.reports
        reports.map { it.severity to it.message }.forEach(::println)
        assertIs<ResultWithDiagnostics.Success<EvaluationResult>>(result, "Script failed to run")

        val returnValue = result.value.returnValue
        assertIs<ResultValue.Value>(returnValue)
        assertEquals(ret, returnValue.value)

        assertEquals("$input\n$quoted", simpleEval.output)
    }
}