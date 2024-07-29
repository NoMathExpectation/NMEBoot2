package NoMathExpectation.NMEBoot.command.util

import com.mojang.brigadier.ParseResults
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.CommandNode
import kotlinx.coroutines.*
import kotlin.reflect.KClass

class SuspendContext<out S> private constructor(
    val source: S,
    private val scope: CoroutineScope,
    private val jobs: MutableList<Job>,
) : CoroutineScope by scope {

    constructor(source: S, scope: CoroutineScope) : this(source, scope, mutableListOf())

    fun launch(block: suspend CoroutineScope.() -> Unit) = scope.launch { block() }.also { jobs += it }

    suspend fun join() = jobs.joinAll()

    fun copyFor(source: @UnsafeVariance S) = SuspendContext(source, scope, jobs)
}

class SuspendableBrigadierCommandContext<out S>(private val context: CommandContext<SuspendContext<S>>) :
    BrigadierCommandContext<S> {
    override val source: S
        get() = context.source.source
    override val input: String
        get() = context.input

    override fun <AT : Any> get(name: String, clazz: KClass<AT>) = kotlin.runCatching {
        context.getArgument(name, clazz.java)
    }.getOrNull()
}

typealias SuspendableCommandDispatcher<S> = DslCommandDispatcher<SuspendContext<S>>
typealias SuspendableParseResults<S> = ParseResults<SuspendContext<S>>
typealias SuspendableArgumentBuilder<S, T> = DslArgumentBuilder<SuspendContext<S>, T>
typealias SuspendableCommandNode<S> = CommandNode<SuspendContext<S>>

inline fun <S> SuspendableCommandDispatcher(block: SuspendableCommandDispatcher<S>.() -> Unit = {}) =
    CommandDispatcher(block)

fun <S> SuspendableCommandDispatcher<S>.parse(scope: CoroutineScope, command: String, source: S) = parse(
    command, SuspendContext(
        source,
        scope
    )
)

fun <S> SuspendableCommandDispatcher<S>.execute(scope: CoroutineScope, command: String, source: S) = parse(
    command, SuspendContext(
        source,
        scope
    )
).run { execute(this) }

suspend fun <S> SuspendableCommandDispatcher<S>.execute(command: String, source: S) = coroutineScope {
    execute(this, command, source)
}

inline fun <S, T : SuspendableArgumentBuilder<S, T>> SuspendableArgumentBuilder<S, T>.filter(crossinline predicate: suspend S.() -> Boolean) =
    requires { runBlocking { predicate(it.source) } }!!

inline fun <S, T : SuspendableArgumentBuilder<S, T>> SuspendableArgumentBuilder<S, T>.handle(crossinline block: suspend BrigadierCommandContext<S>.() -> Unit) =
    executes {
        it.source.launch { SuspendableBrigadierCommandContext(it).block() }
        1
    }

inline fun <S, T : SuspendableArgumentBuilder<S, T>> SuspendableArgumentBuilder<S, T>.targets(
    target: SuspendableCommandNode<S>,
    fork: Boolean = true,
    crossinline modifier: BrigadierCommandContext<S>.() -> List<S> = { listOf(source) }
) =
    forward(target, { SuspendableBrigadierCommandContext(it).modifier().map { t -> it.source.copyFor(t) } }, fork)!!

inline fun <S, T : SuspendableArgumentBuilder<S, T>> SuspendableArgumentBuilder<S, T>.targetsSingle(
    target: SuspendableCommandNode<S>,
    fork: Boolean = false,
    crossinline modifier: BrigadierCommandContext<S>.() -> S = { source }
) =
    forward(target, { listOf(it.source.copyFor(SuspendableBrigadierCommandContext(it).modifier())) }, fork)!!