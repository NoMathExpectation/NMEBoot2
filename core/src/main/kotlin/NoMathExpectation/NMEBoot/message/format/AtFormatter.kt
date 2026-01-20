package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.message.FormatOptions
import NoMathExpectation.NMEBoot.util.nickOrName
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.firstOrNull
import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.component.kook.message.KookMessages
import love.forte.simbot.definition.Actor
import love.forte.simbot.definition.Guild
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
            is User if type == At.DEFAULT_AT_TYPE -> if (target == context.id) context.name else target.toString()
            is Guild if type == KookMessages.AT_TYPE_CHANNEL -> context.channel(target)?.name ?: target.toString()
            is Organization if type == KookMessages.AT_TYPE_ROLE -> context.roles.asFlow()
                .firstOrNull { it.id == target }?.name ?: target.toString()

            is Organization if type == At.DEFAULT_AT_TYPE -> context.member(target)?.nickOrName ?: target.toString()
            null -> target.toString()
            else -> {
                logger.warn { "Unknown type and context for At: $target, $target, $context" }
                target.toString()
            }
        }

        return "$prefix$name"
    }

    override suspend fun toReadableString(element: At, context: Actor?, options: FormatOptions): String {
        return toReadableString(element.target, element.type, context)
    }

    override suspend fun serialize(element: At, context: Actor?, options: FormatOptions): List<String> {
        return listOf(type, element.type, element.target.toString())
    }

    override suspend fun deserialize(segments: List<String>, context: Actor?, options: FormatOptions): At {
        val (_, type, target) = segments
        return At(target.ID, type, toReadableString(target.ID, type, context))
    }
}