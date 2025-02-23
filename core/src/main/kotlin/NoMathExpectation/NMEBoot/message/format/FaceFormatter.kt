package NoMathExpectation.NMEBoot.message.format

import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.Face
import org.koin.core.annotation.Single

@Single
class FaceFormatter : MessageElementFormatter<Face> {
    override val type = "face"
    override val formatClass = Face::class

    override suspend fun toReadableString(element: Face, context: Actor?): String {
        return "[face:${element.id}]"
    }

    override suspend fun serialize(element: Face, context: Actor?): List<String> {
        return listOf(type, element.id.toString())
    }

    override suspend fun deserialize(segments: List<String>, context: Actor?): Face {
        val (_, id) = segments
        return Face(id.ID)
    }
}