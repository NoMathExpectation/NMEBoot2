package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.message.FormatOptions
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.PlainText
import love.forte.simbot.message.Text
import love.forte.simbot.message.toText
import org.koin.core.annotation.Single

@Single
class PlainTextFormatter : MessageElementFormatter<PlainText> {
    override val type = "text"
    override val formatClass = PlainText::class

    override suspend fun toReadableString(
        element: PlainText,
        context: Actor?,
        options: FormatOptions
    ): String {
        return element.text
    }

    override suspend fun serialize(
        element: PlainText,
        context: Actor?,
        options: FormatOptions
    ): List<String> {
        return listOf(type, element.text)
    }

    override suspend fun deserialize(
        segments: List<String>,
        context: Actor?,
        options: FormatOptions
    ): Text {
        return segments[1].toText()
    }
}