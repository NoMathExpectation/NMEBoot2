package NoMathExpectation.NMEBoot.message.format

import love.forte.simbot.definition.Actor
import love.forte.simbot.message.AtAll
import org.koin.core.annotation.Single

@Single
class AtAllFormatter : MessageElementFormatter<AtAll> {
    override val type = "atAll"
    override val formatClass = AtAll::class

    override suspend fun toReadableString(element: AtAll, context: Actor?): String {
        return "@全体成员"
    }

    override suspend fun serialize(element: AtAll, context: Actor?): List<String> {
        return listOf(type)
    }

    override suspend fun deserialize(segments: List<String>, context: Actor?): AtAll {
        return AtAll
    }
}