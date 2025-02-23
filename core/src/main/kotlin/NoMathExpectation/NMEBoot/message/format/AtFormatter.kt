package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.util.nickOrName
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.component.kook.message.KookMessages
import love.forte.simbot.definition.Actor
import love.forte.simbot.definition.Organization
import love.forte.simbot.definition.User
import love.forte.simbot.message.At
import org.koin.core.annotation.Single

@Single
class AtFormatter : MessageElementFormatter<At> {
    override val type = "at"
    override val formatClass = At::class

    private val logger = KotlinLogging.logger { }

    suspend fun toReadableString(target: ID, type: String, context: Actor? = null): String {
        val prefix = when (type) {
            At.DEFAULT_AT_TYPE, KookMessages.AT_TYPE_ROLE -> "@"
            KookMessages.AT_TYPE_CHANNEL -> "#"
            else -> {
                logger.warn { "Unknown at type: $type, defaults to '@'." }
                "@"
            }
        }

        val name = when (context) {
            is User -> if (target == context.id) context.name else target.toString()
            is Organization -> context.member(target)?.nickOrName
            null -> target.toString()
            else -> {
                logger.warn { "Unknown context for At: $context" }
                target.toString()
            }
        }

        return "$prefix$name"
    }

    override suspend fun toReadableString(element: At, context: Actor?): String {
        return toReadableString(element.target, element.type, context)
    }

    override suspend fun serialize(element: At, context: Actor?): List<String> {
        return listOf(type, element.type, element.target.toString())
    }

    override suspend fun deserialize(segments: List<String>, context: Actor?): At {
        val (_, type, target) = segments
        return At(target.ID, type, toReadableString(target.ID, type, context))
    }
}