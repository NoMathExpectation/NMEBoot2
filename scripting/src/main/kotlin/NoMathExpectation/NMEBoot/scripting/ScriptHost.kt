package NoMathExpectation.NMEBoot.scripting

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.InputStream
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

val logger = KotlinLogging.logger { }

data class EvalConfig(
    val sourceCode: SourceCode,
    var input: InputStream,
    var quoted: String? = null,
) {
    class Builder(val sourceCode: SourceCode) {
        var input: InputStream? = null
        var quoted: String? = null

        fun build() = EvalConfig(
            sourceCode,
            input ?: InputStream.nullInputStream(),
            quoted,
        )
    }
}

fun EvalConfig(sourceCode: SourceCode, block: EvalConfig.Builder.() -> Unit = {}): EvalConfig {
    val builder = EvalConfig.Builder(sourceCode)
    builder.block()
    return builder.build()
}

data class EvalResult<out R>(
    val resultWithDiagnostics: ResultWithDiagnostics<R>,
    val output: String,
)

fun SourceCode.eval(block: EvalConfig.Builder.() -> Unit = {}): EvalResult<EvaluationResult> {
    val config = EvalConfig(this, block)

    val fakeConsole = FakeConsole(config.input)

    val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptDefinition>()
    val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ScriptDefinition>().with {
        implicitReceivers(fakeConsole)
        providedProperties("quoted" to config.quoted)
        scriptExecutionWrapper<Any?> {
            it().also { logger.info { "Eval output: " + fakeConsole.output } }
        }
    }

    val resultWithDiagnostics = fakeConsole.use {
        BasicJvmScriptingHost().eval(config.sourceCode, compilationConfiguration, evaluationConfiguration)
    }
    return EvalResult(
        resultWithDiagnostics,
        fakeConsole.output,
    )
}

fun File.evalScript(block: EvalConfig.Builder.() -> Unit = {}) = toScriptSource().eval(block)

fun String.evalScript(block: EvalConfig.Builder.() -> Unit = {}) = toScriptSource().eval(block)