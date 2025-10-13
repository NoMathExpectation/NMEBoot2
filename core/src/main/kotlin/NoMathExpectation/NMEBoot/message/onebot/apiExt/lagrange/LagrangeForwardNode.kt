package NoMathExpectation.NMEBoot.message.onebot.apiExt.lagrange

import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.StringID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.common.id.literal
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegment
import love.forte.simbot.component.onebot.v11.message.segment.OneBotMessageSegmentSerializer

@Serializable
data class LagrangeForwardNode(
    val name: String,
    val uin: StringID,
    @Serializable(OneBotMessageSegmentSerializer::class)
    val content: List<OneBotMessageSegment> = listOf(),
) {
    @Serializable
    class Wrapper(val data: LagrangeForwardNode) {
        val type = "node"
    }
}

fun LagrangeForwardNode.wrap() = LagrangeForwardNode.Wrapper(this)

fun Iterable<LagrangeForwardNode>.wrap() = map { it.wrap() }

fun OneBotForwardNode.Data.toLagrange() = LagrangeForwardNode(
    name = nickname ?: error("name missing"),
    uin = userId?.literal?.ID ?: error("user id missing"),
    content = content ?: error("content missing")
)

fun OneBotForwardNode.toLagrange() = data.toLagrange().wrap()
