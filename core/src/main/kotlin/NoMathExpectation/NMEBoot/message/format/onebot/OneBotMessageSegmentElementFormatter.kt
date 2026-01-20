package NoMathExpectation.NMEBoot.message.format.onebot

import NoMathExpectation.NMEBoot.message.FormatOptions
import NoMathExpectation.NMEBoot.message.format.ImageFormatter
import NoMathExpectation.NMEBoot.message.format.MessageElementFormatter
import NoMathExpectation.NMEBoot.message.format.PlainTextFormatter
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.component.onebot.v11.message.segment.*
import love.forte.simbot.definition.Actor
import org.koin.core.annotation.Single

@Single
class OneBotMessageSegmentElementFormatter(
    private val diceFormatter: OneBotDiceFormatter,
    private val imageFormatter: ImageFormatter,
    private val textFormatter: PlainTextFormatter,
) : MessageElementFormatter<OneBotMessageSegmentElement> {
    override val type = "obs"
    override val formatClass = OneBotMessageSegmentElement::class

    private val logger = KotlinLogging.logger { }

    override suspend fun toReadableString(
        element: OneBotMessageSegmentElement,
        context: Actor?,
        options: FormatOptions
    ): String {
        return "[onebot消息段]"
    }

    override suspend fun serialize(
        element: OneBotMessageSegmentElement,
        context: Actor?,
        options: FormatOptions
    ): List<String> {
        when (element) {
            is OneBotDice.Element -> return diceFormatter.serialize(element, context)
            is OneBotText.Element -> return textFormatter.serialize(element, context)
            is OneBotImage.Element -> return imageFormatter.serialize(element, context)
        }

        return when (val obSegment = element.segment) {
            is OneBotJson -> listOf(type, "json", obSegment.data.data)
            is OneBotXml -> listOf(type, "xml", obSegment.data.data)
            is OneBotReply -> listOf("ref", obSegment.id.toString())
            else -> {
                logger.warn { "Unknown onebot segment: $obSegment" }
                listOf(type, "unknown")
            }
        }
    }

    override suspend fun deserialize(
        segments: List<String>,
        context: Actor?,
        options: FormatOptions
    ): OneBotMessageSegmentElement {
        return when (segments.getOrNull(1)) {
            "json" -> OneBotJson.create(segments[2])
            "xml" -> OneBotXml.create(segments[2])
            "dice" -> OneBotDice
            "text" -> OneBotText.create(segments.getOrNull(2) ?: "")
            "reply" -> OneBotReply.create(segments[2].ID)
            "unknown" -> OneBotText.create("")
            else -> {
                logger.warn { "Unknown onebot segment: $segments" }
                OneBotText.create("")
            }
        }.toElement()
    }
}