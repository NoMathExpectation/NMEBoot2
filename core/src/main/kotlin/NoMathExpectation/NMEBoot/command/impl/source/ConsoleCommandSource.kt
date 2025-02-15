package NoMathExpectation.NMEBoot.command.impl.source

import NoMathExpectation.NMEBoot.command.impl.source.offline.OfflineConsoleCommandSource
import NoMathExpectation.NMEBoot.message.toReadableString
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageReceipt

object ConsoleCommandSource : CommandSource<Nothing?> {
    override val origin = null
    override val uid: Long = 0
    override val id = "console"
    override val platform = "console"
    override val bot = null
    override val globalSubject = null
    override val globalSubjectPermissionId = null
    override val subject = null
    override val subjectPermissionId = null
    override val executor = null

    private val logger = KotlinLogging.logger("Console")

    override suspend fun send(message: Message): MessageReceipt {
        logger.info { message.toReadableString() }
        return UnsupportedDeleteOpMessageReceipt
    }

    override suspend fun reply(message: Message) = send(message)

    override suspend fun toOffline() = OfflineConsoleCommandSource
}