package NoMathExpectation.NMEBoot.scripting

import kotlinx.coroutines.runBlocking
import kotlin.reflect.typeOf
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    return runBlocking {
        resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object ScriptCompileConfig : ScriptCompilationConfiguration({
    defaultImports(DependsOn::class, Repository::class)
    jvm {
        dependenciesFromCurrentContext(wholeClasspath = true)
    }
    refineConfiguration {
        onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
    }
    implicitReceivers(FakeConsole::class)
    providedProperties("quoted" to typeOf<String?>())
})

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
object ScriptEvalConfig : ScriptEvaluationConfiguration({})

@KotlinScript(
    fileExtension = "script.kts",
    compilationConfiguration = ScriptCompileConfig::class,
    evaluationConfiguration = ScriptEvalConfig::class
)
abstract class ScriptDefinition