package NoMathExpectation.NMEBoot.message.format.onebot

import NoMathExpectation.NMEBoot.message.format.MessageElementFormatter
import love.forte.simbot.component.onebot.v11.message.segment.OneBotDice
import love.forte.simbot.definition.Actor
import org.koin.core.annotation.Single

@Single
class OneBotDiceFormatter : MessageElementFormatter<OneBotDice.Element> {
    override val type = "obdice"
    override val formatClass = OneBotDice.Element::class

    override suspend fun toReadableString(
        element: OneBotDice.Element,
        context: Actor?
    ): String {
        return "[骰子]"
    }

    override suspend fun serialize(
        element: OneBotDice.Element,
        context: Actor?
    ): List<String> {
        return listOf(type)
    }

    override suspend fun deserialize(
        segments: List<String>,
        context: Actor?
    ): OneBotDice.Element {
        return OneBotDice.Element
    }
}