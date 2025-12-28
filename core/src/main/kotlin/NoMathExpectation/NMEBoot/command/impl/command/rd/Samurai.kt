package NoMathExpectation.NMEBoot.command.impl.command.rd

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.requiresSubjectId
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.message.event.CommandSourcePreSendEvent
import NoMathExpectation.NMEBoot.message.message
import NoMathExpectation.NMEBoot.util.asMessages
import NoMathExpectation.NMEBoot.util.mutableMapStorageOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import love.forte.simbot.event.InteractionMessage
import love.forte.simbot.message.*

const val SAMURAI_PERMISSION_NAME = "command.rd.fanmade.samurai"

private val logger = KotlinLogging.logger { }

data object SamuraiIgnore : Message.Element

@Serializable
data class Samurai(var enabled: Boolean = false) {
    companion object {
        private val config = mutableMapStorageOf<String, _>("config/samurai.json") { Samurai() }
        val words = listOf("Samurai.", "Donut.", " Great.", "Nice.")

        suspend fun <R> updateConfig(key: String, update: Samurai.() -> R): R {
            return config.referenceUpdate(key) { it.update() }
        }

        suspend fun getConfig(key: String): Samurai {
            return config.get(key)
        }

        suspend fun exception(key: String): Message {
            updateConfig(key) { enabled = true }
            return exceptionMessage
        }

        val randomWord get() = words.random()
        val exceptionMessage get() = SamuraiIgnore + "武士走进了妮可的咖啡店，要了一份甜甜圈。\n$randomWord".toText()
    }
}

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandSamurai() =
    literal("samurai")
        .requiresPermission(SAMURAI_PERMISSION_NAME)
        .requiresSubjectId()
        .executes("Samurai.") {
            val subjectId = it.target.subjectPermissionId ?: error("missing subject id")
            val enabled = Samurai.updateConfig(subjectId) {
                enabled = !enabled
                enabled
            }

            if (enabled) {
                logger.info { "武士进入了咖啡店：$subjectId" }
                it.reply(Samurai.randomWord)
            } else {
                logger.info { "武士离开了咖啡店：$subjectId" }
                it.reply("武士离开了咖啡店。")
            }
        }

suspend fun CommandSource<*>.isSamuraiMode(): Boolean {
    return hasPermission(SAMURAI_PERMISSION_NAME) && Samurai.getConfig(
        subjectPermissionId ?: return false
    ).enabled
}

suspend fun handleSamurai(event: CommandSourcePreSendEvent<*>) {
    val source = event.content
    if (!source.isSamuraiMode()) {
        return
    }

    val currentMessage = event.currentMessage.message.asMessages()
    if (SamuraiIgnore in currentMessage) {
        return
    }

    event.currentMessage = InteractionMessage.valueOf(currentMessage.map {
        if (it is PlainText) {
            return@map it.text.split("\n").joinToString("\n") { Samurai.randomWord }.toText()
        }
        it
    }.toMessages())
}