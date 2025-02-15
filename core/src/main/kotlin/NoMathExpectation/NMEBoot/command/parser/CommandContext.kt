package NoMathExpectation.NMEBoot.command.parser

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.safeCast

class CommandContext<out S>(
    val source: S,
    val reader: StringReader,
) {
    private val arguments = mutableMapOf<String, Any?>()

    fun copy(source: @UnsafeVariance S = this.source): CommandContext<S> {
        val copy = CommandContext(source, reader.copy())
        copy.arguments.putAll(arguments)
        return copy
    }

    operator fun set(name: String, value: Any?) {
        arguments[name] = value
    }

    operator fun <T : Any> get(name: String, kClass: KClass<T>): T? {
        return kClass.safeCast(arguments[name])
    }

    fun <T : Any> argument(kClass: KClass<T>) = object : ReadWriteProperty<Any?, T?>{
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            this@CommandContext[property.name] = value
        }

        override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
            return get(property.name, kClass)
        }
    }
}

inline operator fun <reified T : Any> CommandContext<*>.get(name: String): T? {
    return get(name, T::class)
}

inline fun <reified T : Any> CommandContext<*>.argument() = argument(T::class)

