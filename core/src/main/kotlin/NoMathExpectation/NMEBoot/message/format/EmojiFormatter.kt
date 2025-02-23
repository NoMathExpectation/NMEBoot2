package NoMathExpectation.NMEBoot.message.format

import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.Emoji
import org.koin.core.annotation.Single

@Single
class EmojiFormatter : MessageElementFormatter<Emoji> {
    override val type = "emoji"
    override val formatClass = Emoji::class

    override suspend fun toReadableString(element: Emoji, context: Actor?): String {
        return "[emoji:${element.id}]"
    }

    override suspend fun serialize(element: Emoji, context: Actor?): List<String> {
        return listOf(type, element.id.toString())
    }

    override suspend fun deserialize(segments: List<String>, context: Actor?): Emoji {
        val (_, id) = segments
        return Emoji(id.ID)
    }
}