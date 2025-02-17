package NoMathExpectation.NMEBoot.scripting

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.InputStream
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

private val logger = KotlinLogging.logger { }

class SimpleEval(
    val sourceCode: SourceCode,
    val input: InputStream,
    val quoted: String? = null,
) : AutoCloseable {
    class Builder(val sourceCode: SourceCode) {
        var input: InputStream? = null
        var quoted: String? = null

        fun build() = SimpleEval(
            sourceCode,
            input ?: InputStream.nullInputStream(),
            quoted,
        )
    }

    val proxyConsole = ProxyConsole(input)
    var result: ResultWithDiagnostics<EvaluationResult>? = null
        private set
    val output get() = proxyConsole.output

    var invoked = false
        private set

    operator fun invoke() {
        if (invoked) {
            error("This instance is already invoked.")
        }
        invoked = true

        use {
            val compilationConfiguration = createJvmCompilationConfigurationFromTemplate<ScriptDefinition>()
            val evaluationConfiguration = createJvmEvaluationConfigurationFromTemplate<ScriptDefinition>().with {
                implicitReceivers(proxyConsole)
                providedProperties("quoted" to quoted)
                scriptExecutionWrapper<Any?> {
                    it().also { logger.info { "Eval output: " + proxyConsole.output } }
                }
            }

            result = BasicJvmScriptingHost().eval(sourceCode, compilationConfiguration, evaluationConfiguration)
        }
    }

    override fun close() {
        proxyConsole.close()
    }
}

fun SimpleEval(sourceCode: SourceCode, block: SimpleEval.Builder.() -> Unit = {}): SimpleEval {
    val builder = SimpleEval.Builder(sourceCode)
    builder.block()
    return builder.build()
}

fun SourceCode.toSimpleEval(block: SimpleEval.Builder.() -> Unit = {}) = SimpleEval(this, block)

fun File.toSimpleEval(block: SimpleEval.Builder.() -> Unit = {}) = toScriptSource().toSimpleEval(block)

fun String.toSimpleEval(block: SimpleEval.Builder.() -> Unit = {}) = toScriptSource().toSimpleEval(block)