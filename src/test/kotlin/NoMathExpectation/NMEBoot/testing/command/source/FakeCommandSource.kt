package NoMathExpectation.NMEBoot.testing.command.source

import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.message.toReadableString
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageReceipt
import kotlin.random.Random

class FakeCommandSource : CommandSource<Nothing?> {
    override val origin = null
    override val uid = Random.nextLong()
    override val id = "fake"
    override val platform = "fake"
    override val globalSubject = null
    override val subject = null
    override val executor = null

    private val logger = KotlinLogging.logger { }

    val receivedSendMessage: MutableList<Message> = mutableListOf()
    override suspend fun sendRaw(message: Message): MessageReceipt? {
        receivedSendMessage.add(message)
        logger.info { "sendRaw: ${message.toReadableString()}" }
        return null
    }

    val receivedReplyMessage: MutableList<Message> = mutableListOf()
    override suspend fun replyRaw(message: Message): MessageReceipt? {
        receivedReplyMessage.add(message)
        logger.info { "replyRaw: ${message.toReadableString()}" }
        return null
    }
}