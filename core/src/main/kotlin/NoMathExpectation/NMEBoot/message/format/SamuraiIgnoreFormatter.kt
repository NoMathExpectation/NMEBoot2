package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.command.impl.command.rd.SamuraiIgnore
import NoMathExpectation.NMEBoot.message.FormatOptions
import love.forte.simbot.definition.Actor
import org.koin.core.annotation.Single

@Single
class SamuraiIgnoreFormatter : MessageElementFormatter<SamuraiIgnore> {
    override val type = "samuraiIgnore"
    override val formatClass = SamuraiIgnore::class

    override suspend fun toReadableString(
        element: SamuraiIgnore,
        context: Actor?,
        options: FormatOptions
    ): String {
        return "[Samurai Ignored]"
    }

    override suspend fun serialize(
        element: SamuraiIgnore,
        context: Actor?,
        options: FormatOptions
    ): List<String> {
        return listOf(type)
    }

    override suspend fun deserialize(
        segments: List<String>,
        context: Actor?,
        options: FormatOptions
    ): SamuraiIgnore {
        return SamuraiIgnore
    }
}