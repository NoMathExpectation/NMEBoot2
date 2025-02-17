package NoMathExpectation.NMEBoot.scripting

import NoMathExpectation.NMEBoot.scripting.data.EvalRequest
import NoMathExpectation.NMEBoot.scripting.data.EvalResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic

private val logger = KotlinLogging.logger { }

private val threadPool = Executors.newCachedThreadPool()

private val stackTraceEndRegex = "\\s+at\\s+Script\\.<init>\\(.*?\\.kts:\\d+\\)".toRegex()

fun Routing.evalService() = post<EvalRequest>("/eval") { request ->
    logger.info {
        """
        Attempting to eval the following script:
        Code: ${request.source}
        input: ${request.input}
        quoted: ${request.quoted}
        """.trimIndent()
    }

    val simpleEval = request.source.toSimpleEval {
        input = request.input?.byteInputStream()
        quoted = request.quoted
    }

    var tle = false
    val future = threadPool.submit(simpleEval::invoke)
    withTimeoutOrNull(request.timeout) {
        while (isActive && !future.isDone) {
            delay(1000)
        }
    } ?: run {
        tle = true
        future.cancel(true)
    }

    if (tle) {
        call.respond(
            EvalResponse(
                simpleEval.output,
                null,
                "Time limit exceeded."
            )
        )
        return@post
    }

    val resultWithDiagnostics = simpleEval.result

    if (resultWithDiagnostics is ResultWithDiagnostics.Failure) {
        resultWithDiagnostics.reports
            .filter { it.severity >= ScriptDiagnostic.Severity.WARNING }
            .forEach {
                logger.warn(it.exception) { "${it.severity} from script diagnostic: ${it.message}" }
            }

        val reportString = resultWithDiagnostics.reports
            .filter { it.severity >= ScriptDiagnostic.Severity.WARNING }
            .joinToString("\n") { "${it.severity}: ${it.message}" }

        call.respond(
            EvalResponse(
                simpleEval.output,
                null,
                reportString
            )
        )
        return@post
    }

    if (resultWithDiagnostics is ResultWithDiagnostics.Success<EvaluationResult>) {
        var returns: String? = null
        var exception: String? = null

        when (val returnValue = resultWithDiagnostics.value.returnValue) {
            is ResultValue.Error -> {
                logger.warn(returnValue.error) { "Error inside the script." }
                exception = returnValue.error
                    .stackTraceToString()
                    .lines()
                    .dropLastWhile { !stackTraceEndRegex.matches(it) }
                    .joinToString("\n")
            }

            is ResultValue.Unit -> {}
            is ResultValue.Value -> returns = returnValue.value.toString()
            ResultValue.NotEvaluated -> exception = "Script not evaluated."
        }

        call.respond(
            EvalResponse(
                simpleEval.output,
                returns,
                exception
            )
        )
        return@post
    }

    call.respond(
        EvalResponse(
            simpleEval.output,
            null,
            "Unable to resolve the result."
        )
    )
}