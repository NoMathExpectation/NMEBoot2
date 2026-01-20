package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.message.FormatOptions
import NoMathExpectation.NMEBoot.message.onebot.OneBotFolding
import love.forte.simbot.definition.Actor
import org.koin.core.annotation.Single

@Single
class FoldIgnoreFormatter : MessageElementFormatter<OneBotFolding.FoldIgnore> {
    override val type = "foldIgnore"
    override val formatClass = OneBotFolding.FoldIgnore::class

    override suspend fun toReadableString(
        element: OneBotFolding.FoldIgnore,
        context: Actor?,
        options: FormatOptions
    ): String {
        return "[Folding Ignored]"
    }

    override suspend fun serialize(
        element: OneBotFolding.FoldIgnore,
        context: Actor?,
        options: FormatOptions
    ): List<String> {
        return listOf(type)
    }

    override suspend fun deserialize(
        segments: List<String>,
        context: Actor?,
        options: FormatOptions
    ): OneBotFolding.FoldIgnore {
        return OneBotFolding.FoldIgnore
    }
}