package NoMathExpectation.NMEBoot.command.util

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.ArgumentCommandNode
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@DslMarker
annotation class BrigadierDsl

typealias DslCommandDispatcher<S> = @BrigadierDsl CommandDispatcher<S>

typealias DslLiteralArgumentBuilder<S> = @BrigadierDsl LiteralArgumentBuilder<S>

typealias DslRequiredArgumentBuilder<S, T> = @BrigadierDsl RequiredArgumentBuilder<S, T>

typealias DslArgumentBuilder<S, T> = @BrigadierDsl ArgumentBuilder<S, T>

typealias DslCommandContext<S> = @BrigadierDsl CommandContext<S>

@BrigadierDsl
interface BrigadierCommandContext<out S> {
    val source: S
    val input: String

    operator fun <AT : Any> get(name: String, clazz: KClass<AT>): AT?
}

inline fun <reified AT : Any> BrigadierCommandContext<*>.get(name: String) = this[name, AT::class]

inline fun <reified AT : Any> BrigadierCommandContext<*>.getOrFail(name: String) =
    get<AT>(name) ?: throw NoSuchElementException(name)

inline fun <reified AT : Any> BrigadierCommandContext<*>.getOrDefault(name: String, default: AT) =
    get<AT>(name) ?: default

inline fun <reified AT : Any> BrigadierCommandContext<*>.getOrElse(name: String, block: (String) -> AT) =
    get<AT>(name) ?: block(name)

class SingleBrigadierCommandContext<S>(private val brigadierContext: CommandContext<S>) : BrigadierCommandContext<S> {
    override val source: S get() = brigadierContext.source
    override val input: String get() = brigadierContext.input
    override fun <AT : Any> get(name: String, clazz: KClass<AT>) = kotlin.runCatching {
        brigadierContext.getArgument(name, clazz.java)
    }.getOrNull()
}

inline fun <S> CommandDispatcher(block: DslCommandDispatcher<S>.() -> Unit = {}) =
    CommandDispatcher<S>().apply(block)

inline fun <S> literal(
    name: String,
    block: DslLiteralArgumentBuilder<S>.() -> Unit = {}
): LiteralArgumentBuilder<S> =
    LiteralArgumentBuilder.literal<S>(name).apply(block)

inline fun <S, AT> requiredArgument(
    name: String,
    type: ArgumentType<AT>,
    block: DslRequiredArgumentBuilder<S, AT>.() -> Unit = {}
): RequiredArgumentBuilder<S, AT> =
    RequiredArgumentBuilder.argument<S, AT>(name, type).apply(block)

inline fun <S, T1 : ArgumentBuilder<S, T1>, T2 : ArgumentBuilder<S, T2>> DslArgumentBuilder<S, T1>.argument(
    argument: ArgumentBuilder<S, T2>,
    block: DslArgumentBuilder<S, T2>.() -> Unit = {}
): CommandNode<S> =
    argument.apply(block).build().also { then(it) }

inline fun <S, T : ArgumentBuilder<S, T>, AT> DslArgumentBuilder<S, T>.argument(
    name: String,
    type: ArgumentType<AT>,
    block: DslRequiredArgumentBuilder<S, AT>.() -> Unit = {}
): ArgumentCommandNode<S, AT> =
    requiredArgument(name, type, block).build().also { then(it) }

inline fun <S> DslCommandDispatcher<S>.register(
    name: String,
    vararg aliases: String,
    block: DslLiteralArgumentBuilder<S>.() -> Unit = {}
): LiteralCommandNode<S> =
    register(literal(name, block)).also { node ->
        aliases.forEach {
            register(literal(it) {
                targetsSingle(node)
            })
        }
    }

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.literalArgument(
    name: String,
    vararg aliases: String,
    block: DslLiteralArgumentBuilder<S>.() -> Unit = {}
): LiteralCommandNode<S> =
    literal(name, block).build().also { node ->
        then(node)
        aliases.forEach {
            then(literal(it) {
                targetsSingle(node)
            })
        }
    }

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.boolArgument(
    name: String,
    block: DslRequiredArgumentBuilder<S, Boolean>.() -> Unit = {}
) =
    argument(name, BoolArgumentType.bool(), block)

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.doubleArgument(
    name: String,
    min: Double = -Double.MAX_VALUE,
    max: Double = Double.MAX_VALUE,
    block: DslRequiredArgumentBuilder<S, Double>.() -> Unit = {}
) =
    argument(name, DoubleArgumentType.doubleArg(min, max), block)

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.floatArgument(
    name: String,
    min: Float = -Float.MAX_VALUE,
    max: Float = Float.MAX_VALUE,
    block: DslRequiredArgumentBuilder<S, Float>.() -> Unit = {}
) =
    argument(name, FloatArgumentType.floatArg(min, max), block)

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.intArgument(
    name: String,
    min: Int = Int.MIN_VALUE,
    max: Int = Int.MAX_VALUE,
    block: DslRequiredArgumentBuilder<S, Int>.() -> Unit = {}
) =
    argument(name, IntegerArgumentType.integer(min, max), block)

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.longArgument(
    name: String,
    min: Long = Long.MIN_VALUE,
    max: Long = Long.MAX_VALUE,
    block: DslRequiredArgumentBuilder<S, Long>.() -> Unit = {}
) =
    argument(name, LongArgumentType.longArg(min, max), block)

object RefinedStringArgumentType : ArgumentType<String> by StringArgumentType.string() {
    override fun parse(reader: StringReader): String = with(reader) {
        if (!canRead()) {
            return ""
        }

        val next = peek()
        if (StringReader.isQuotedStringStart(next)) {
            skip()
            return readStringUntil(next)
        }

        val start = cursor
        while (canRead() && peek() != ' ') {
            skip()
        }

        return string.substring(start, cursor)
    }
}

enum class StringArgumentCaptureType {
    WORD, QUOTABLE, QUOTABLE_LIMITED, GREEDY
}

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.stringArgument(
    name: String,
    type: StringArgumentCaptureType = StringArgumentCaptureType.QUOTABLE,
    block: DslRequiredArgumentBuilder<S, String>.() -> Unit = {}
) =
    when (type) {
        StringArgumentCaptureType.WORD -> StringArgumentType.word()
        StringArgumentCaptureType.QUOTABLE -> RefinedStringArgumentType
        StringArgumentCaptureType.QUOTABLE_LIMITED -> StringArgumentType.string()
        StringArgumentCaptureType.GREEDY -> StringArgumentType.greedyString()
    }.let { argument(name, it, block) }

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.intHandle(crossinline block: BrigadierCommandContext<S>.() -> Int) =
    executes { SingleBrigadierCommandContext(it).block() }!!

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.boolHandle(crossinline block: BrigadierCommandContext<S>.() -> Boolean) =
    executes { if (SingleBrigadierCommandContext(it).block()) 1 else 0 }!!

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.handle(crossinline block: BrigadierCommandContext<S>.() -> Unit) =
    executes { SingleBrigadierCommandContext(it).block(); 1 }!!

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.filter(crossinline predicate: S.() -> Boolean) =
    requires { predicate(it) }!!

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.targets(
    target: CommandNode<S>,
    fork: Boolean = true,
    crossinline modifier: BrigadierCommandContext<S>.() -> List<S> = { listOf(source) }
) =
    forward(target, { SingleBrigadierCommandContext(it).modifier() }, fork)!!

inline fun <S, T : ArgumentBuilder<S, T>> DslArgumentBuilder<S, T>.targetsSingle(
    target: CommandNode<S>,
    fork: Boolean = false,
    crossinline modifier: BrigadierCommandContext<S>.() -> S = { source }
) =
    targets(target, fork) { listOf(modifier()) }