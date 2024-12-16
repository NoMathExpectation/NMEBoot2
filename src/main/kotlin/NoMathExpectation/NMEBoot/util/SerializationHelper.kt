package NoMathExpectation.NMEBoot.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModuleBuilder
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

private interface TransformStrategy<Base : Any, From : Base, out To : Base> : SerializationStrategy<From> {
    val fromKClass: KClass<From>
}

private class TransformEncodeStrategy<Base : Any, From : Base, out To : Base>(
    override val fromKClass: KClass<From>,
    override val descriptor: SerialDescriptor,
    private val serializeFunction: SerializationStrategy<From>.(Encoder, From) -> Unit,
) : TransformStrategy<Base, From, To> {
    override fun serialize(encoder: Encoder, value: From) = serializeFunction(encoder, value)
}

private class TransformDelegateStrategy<Base : Any, From : Base, out To : Base>(
    override val fromKClass: KClass<From>,
    private val delegateSerializer: SerializationStrategy<To>,
    private val transformFunction: (From) -> To,
) : TransformStrategy<Base, From, To> {
    override val descriptor: SerialDescriptor
        get() = delegateSerializer.descriptor

    override fun serialize(encoder: Encoder, value: From) {
        encoder.encodeSerializableValue(delegateSerializer, transformFunction(value))
    }
}

class PolymorphicTransformBuilder<Base : Any>(
    val baseKClass: KClass<Base>,
    val baseKSerializer: KSerializer<Base>? = null,
) {
    inner class Transforms<To : Base>(
        val toKClass: KClass<To>,
        val toKSerializer: KSerializer<To>,
    ) {
        private val serializationStrategies = mutableListOf<TransformStrategy<Base, *, To>>()

        fun <From : Base> fromEncoding(
            fromKClass: KClass<From>,
            descriptor: SerialDescriptor,
            serializeFunction: SerializationStrategy<From>.(Encoder, From) -> Unit,
        ) {
            serializationStrategies += TransformEncodeStrategy(fromKClass, descriptor, serializeFunction)
        }

        inline fun <reified From : Base> fromEncoding(
            descriptor: SerialDescriptor = toKSerializer.descriptor,
            noinline serializeFunction: SerializationStrategy<From>.(Encoder, From) -> Unit,
        ) = fromEncoding(From::class, descriptor, serializeFunction)

        fun <From : Base> fromDelegate(
            fromKClass: KClass<From>,
            delegateSerializer: SerializationStrategy<To>,
            transformFunction: (From) -> To,
        ) {
            serializationStrategies += TransformDelegateStrategy(fromKClass, delegateSerializer, transformFunction)
        }

        inline fun <reified From : Base> fromDelegate(
            delegateSerializer: SerializationStrategy<To> = toKSerializer,
            noinline transformFunction: (From) -> To,
        ) = fromDelegate(From::class, delegateSerializer, transformFunction)

        fun retrieveStrategy(instance: Base): SerializationStrategy<Base>? {
            serializationStrategies.forEach {
                if (it.fromKClass.isInstance(instance)) {
                    @Suppress("UNCHECKED_CAST")
                    return it as SerializationStrategy<Base>
                }
            }
            return null
        }
    }

    private val transforms = mutableListOf<Transforms<out Base>>()

    fun <To : Base> to(
        toKClass: KClass<To>,
        toKSerializer: KSerializer<To>,
        block: Transforms<To>.() -> Unit = {},
    ) {
        transforms += Transforms(toKClass, toKSerializer).apply(block)
    }

    inline fun <reified To : Base> to(
        noinline block: Transforms<To>.() -> Unit = {},
    ) = to(To::class, serializer<To>(), block)

    fun retrieveStrategy(instance: Base): SerializationStrategy<Base>? {
        transforms.forEach {
            it.retrieveStrategy(instance)?.let { strategy ->
                return strategy
            }
        }
        return null
    }

    fun buildTo(builder: SerializersModuleBuilder) {
        builder.polymorphicDefaultSerializer(baseKClass, ::retrieveStrategy)

        builder.polymorphic(baseKClass, baseKSerializer) {
            @Suppress("UNCHECKED_CAST")
            fun <Base : Any, T : Base> PolymorphicModuleBuilder<Base>.castSubclass(
                kClass: KClass<T>,
                kSerializer: KSerializer<out Base>
            ) = subclass(kClass, kSerializer as KSerializer<T>)

            transforms.forEach {
                castSubclass(it.toKClass, it.toKSerializer)
            }
        }
    }
}

inline fun <reified Base : Any> SerializersModuleBuilder.polymorphicTransform(
    baseKClass: KClass<Base> = Base::class,
    baseKSerializer: KSerializer<Base>? = null,
    block: PolymorphicTransformBuilder<Base>.() -> Unit,
) = PolymorphicTransformBuilder(baseKClass, baseKSerializer).apply(block).buildTo(this)