package NoMathExpectation.NMEBoot.message.onebot.apiExt.napcat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.StringID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.common.id.literal
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegment
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegmentSerializer

@Serializable
data class NapcatForwardNode(
    val nickname: String,
    @SerialName("user_id")
    val userId: StringID,
    @Serializable(OneBotMessageSegmentSerializer::class)
    val content: List<OneBotMessageSegment> = listOf(),
) {
    @Serializable
    class Wrapper(val data: NapcatForwardNode, val type: String)
}

fun NapcatForwardNode.wrap() = NapcatForwardNode.Wrapper(this, "node")

fun Iterable<NapcatForwardNode>.wrap() = map { it.wrap() }

fun OneBotForwardNode.Data.toNapcat() = NapcatForwardNode(
    nickname = nickname ?: error("name missing"),
    userId = userId?.literal?.ID ?: error("user id missing"),
    content = content ?: error("content missing")
)

fun OneBotForwardNode.toNapcat() = data.toNapcat().wrap()
