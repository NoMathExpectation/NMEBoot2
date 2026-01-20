package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.message.FormatOptions
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.MessageIdReference
import love.forte.simbot.message.MessageReference
import org.koin.core.annotation.Single

@Single
class MessageReferenceFormatter : MessageElementFormatter<MessageReference> {
    override val type: String = "ref"
    override val formatClass = MessageReference::class

    override suspend fun toReadableString(element: MessageReference, context: Actor?, options: FormatOptions): String {
        return "[引用]"
    }

    override suspend fun serialize(element: MessageReference, context: Actor?, options: FormatOptions): List<String> {
        return listOf("ref", element.id.toString())
    }

    override suspend fun deserialize(
        segments: List<String>,
        context: Actor?,
        options: FormatOptions
    ): MessageReference {
        val (_, id) = segments
        return MessageIdReference(id.ID)
    }
}