package NoMathExpectation.NMEBoot.scripting

import java.io.File
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate
import kotlin.script.experimental.jvmhost.createJvmEvaluationConfigurationFromTemplate

fun createCompilationConfiguration() = createJvmCompilationConfigurationFromTemplate<ScriptDefinition>()

fun createEvaluationConfiguration() = createJvmEvaluationConfigurationFromTemplate<ScriptDefinition>()

fun File.evalScript(): ResultWithDiagnostics<EvaluationResult> {
    val compilationConfiguration = createCompilationConfiguration()
    val evaluationConfiguration = createEvaluationConfiguration()
    return BasicJvmScriptingHost().eval(toScriptSource(), compilationConfiguration, evaluationConfiguration)
}

fun String.evalScript(): ResultWithDiagnostics<EvaluationResult> {
    val compilationConfiguration = createCompilationConfiguration()
    val evaluationConfiguration = createEvaluationConfiguration()
    return BasicJvmScriptingHost().eval(toScriptSource(), compilationConfiguration, evaluationConfiguration)
}