package NoMathExpectation.NMEBoot.command.source

import NoMathExpectation.NMEBoot.util.toReadableString
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.common.id.IntID.Companion.ID
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageReceipt

object ConsoleCommandSource : CommandSource<Nothing?> {
    override val origin = null
    override val uid = 0.ID
    override val id = "console"
    override val platform = "console"
    override val globalSubject = null
    override val subject = null
    override val executor = null

    private val logger = KotlinLogging.logger("Console")

    override suspend fun sendRaw(message: Message): MessageReceipt? {
        logger.info { message.toReadableString() }
        return null
    }

    override suspend fun replyRaw(message: Message) = sendRaw(message)
}