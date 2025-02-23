package NoMathExpectation.NMEBoot.message.format

import love.forte.simbot.definition.Actor
import love.forte.simbot.message.Message
import kotlin.reflect.KClass

typealias SerializedMessage = String

interface MessageElementFormatter<E : Message.Element> {
    val type: String
    val formatClass: KClass<E>

    suspend fun toReadableString(element: E, context: Actor? = null): String

    suspend fun serialize(element: E, context: Actor? = null): List<String>

    suspend fun deserialize(segments: List<String>, context: Actor? = null): E
}

suspend fun <E : Message.Element> MessageElementFormatter<E>.serializeToString(element: E) =
    serialize(element)
        .joinToString(":", "[", "]")