package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.message.onebot.OneBotFolding
import love.forte.simbot.definition.Actor
import org.koin.core.annotation.Single

@Single
class FoldIgnoreFormatter : MessageElementFormatter<OneBotFolding.FoldIgnore> {
    override val type = "foldIgnore"
    override val formatClass = OneBotFolding.FoldIgnore::class

    override suspend fun toReadableString(
        element: OneBotFolding.FoldIgnore,
        context: Actor?
    ): String {
        return "[Folding Ignored]"
    }

    override suspend fun serialize(
        element: OneBotFolding.FoldIgnore,
        context: Actor?
    ): List<String> {
        return emptyList()
    }

    override suspend fun deserialize(
        segments: List<String>,
        context: Actor?
    ): OneBotFolding.FoldIgnore {
        return OneBotFolding.FoldIgnore
    }
}