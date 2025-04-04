package NoMathExpectation.NMEBoot.testing.command.source

import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.impl.source.UnsupportedDeleteOpMessageReceipt
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
    override val bot = null
    override val globalSubject = null
    override val globalSubjectPermissionId = null
    override val subject = null
    override val subjectPermissionId = null
    override val executor = null

    private val logger = KotlinLogging.logger { }

    override suspend fun toOffline() =
        throw UnsupportedOperationException("FakeCommandSource cannot be converted to OfflineCommandSource")

    val receivedSendMessage: MutableList<Message> = mutableListOf()
    override suspend fun send(message: Message): MessageReceipt {
        receivedSendMessage.add(message)
        val text = message.toReadableString(globalSubject)
        logger.info { "sendRaw: $text" }
        return UnsupportedDeleteOpMessageReceipt
    }

    val receivedReplyMessage: MutableList<Message> = mutableListOf()
    override suspend fun reply(message: Message): MessageReceipt {
        receivedReplyMessage.add(message)
        val text = message.toReadableString(globalSubject)
        logger.info { "replyRaw: $text" }
        return UnsupportedDeleteOpMessageReceipt
    }
}