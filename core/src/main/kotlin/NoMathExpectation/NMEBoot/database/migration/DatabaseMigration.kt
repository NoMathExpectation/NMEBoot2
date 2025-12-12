package NoMathExpectation.NMEBoot.database.migration

import NoMathExpectation.NMEBoot.database.DatabaseManager
import NoMathExpectation.NMEBoot.database.message.LegacyMessageHistory
import NoMathExpectation.NMEBoot.database.message.MessageHistory
import NoMathExpectation.NMEBoot.message.escapeMessageFormatIdentifiers
import NoMathExpectation.NMEBoot.user.UIDManager
import NoMathExpectation.NMEBoot.util.splitByUnescapedPaired
import NoMathExpectation.NMEBoot.util.splitUnescaped
import NoMathExpectation.NMEBoot.util.toInstant
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format.char
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

object DatabaseMigration {
    private val logger = KotlinLogging.logger { }

    private fun updateLegacyMessage(message: String): String {
        return message
            .splitByUnescapedPaired('[', ']')
            .map {
                if (!it.startsWith("[")) {
                    return@map it
                }

                val segments = it.trimStart('[')
                    .trimEnd(']')
                    .splitUnescaped(':')
                if (segments.getOrNull(0) != "mirai") {
                    return@map it
                }

                when (segments.getOrNull(1)) {
                    "at" -> {
                        val target = segments.getOrNull(2) ?: run {
                            logger.warn { "Incorrect message format: $it" }
                            return@map it
                        }
                        "[at:user:$target]"
                    }

                    "atall" -> "[atAll]"

                    "image" -> {
                        val image = segments.getOrNull(2) ?: run {
                            logger.warn { "Incorrect message format: $it" }
                            return@map it
                        }
                        val imageId = image.substring(1..36).lowercase()

                        "[image:id:$imageId]"
                    }

                    "face" -> {
                        val faceId = segments.getOrNull(2) ?: run {
                            logger.warn { "Incorrect message format: $it" }
                            return@map it
                        }
                        "[face:$faceId]"
                    }

                    else -> {
                        logger.warn { "Unknown message type: $it" }
                        it
                    }
                }
            }.joinToString("")
    }

    @OptIn(ExperimentalTime::class)
    suspend fun migrateLegacyMessageHistory(legacyJdbcUrl: String, botId: Long) {
//        val botId = simbotApplication?.oneBot11Bots { all().firstOrNull() }?.id?.toLong()
//            ?: error("No OneBot found for migration.")
        val batch = 10000L

        val legacyDataSource = DatabaseManager.createDataSource(legacyJdbcUrl)
        legacyDataSource.use {
            val legacyDatabase = Database.connect(legacyDataSource)
            val mainDatabase = DatabaseManager.mainDatabase

            val count = transaction(legacyDatabase) {
                LegacyMessageHistory.all().count()
            }

            logger.info { "Fetching uids..." }
            val idToUidMap = mutableMapOf<Long, Long>()
            var now = Clock.System.now()
            (0L..<count step batch).forEach { offset ->
                transaction(legacyDatabase) {
                    LegacyMessageHistory.all().offset(offset).limit(batch.toInt()).map { it.sender }
                }.forEach {
                    idToUidMap.getOrPut(it) { UIDManager.fromId("onebot-$it") }
                }

                val now2 = Clock.System.now()
                val fetched = offset + batch
                if (now2 - now > 10.seconds) {
                    logger.info { "Fetched: $fetched/$count (%.2f%%)".format(fetched.toDouble() * 100 / count) }
                    now = now2
                }
            }

            logger.info { "Migrating old database..." }
            now = Clock.System.now()
            (0L..<count step batch).forEach { offset ->
                transaction(mainDatabase) {
                    transaction(legacyDatabase) {
                        LegacyMessageHistory.all().offset(offset).limit(batch.toInt()).toList()
                    }.forEach {
                        MessageHistory.new {
                            platform = "onebot"
                            this.botId = botId.toString()

                            if (it.group != null) {
                                globalSubjectId = "${it.group}"
                                globalSubjectName = "<unknown>"

                                subjectId = globalSubjectId
                                subjectName = globalSubjectName
                            }

                            senderId = it.sender.toString()
                            senderName = it.name
                            senderUid = idToUidMap[it.sender]!!

                            messageId = "legacy-${it.ids}"
                            message = updateLegacyMessage(it.message)

                            time = Instant.fromEpochMilliseconds(it.time)

                            isBot = it.sender == botId

                            version = 1
                        }
                    }
                }

                val migrated = offset + batch
                val now2 = Clock.System.now()
                if (now2 - now > 10.seconds) {
                    logger.info { "Migrated: $migrated/$count (%.2f%%)".format(migrated.toDouble() * 100 / count) }
                    now = now2
                }
            }
        }

        logger.info { "Migration complete." }
    }

    private val logStartRegex =
        "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (TRACE|DEBUG|INFO|WARN|ERROR)".toRegex()
    private val messageStartRegex =
        "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) INFO {2}Messages - ".toRegex()
    private val messageRegex =
        "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) INFO {2}Messages - Bot\\.(.*?)\\.([rt]x) (?:\\[(.*?)\\((\\d*?)\\)])?(?:\\[(.*?)\\((\\d*?)\\)])?(?:\\[(private|contact|unknown)])? (?:(.*?)\\((\\d*?)\\) )?(?:->|<-) (.*?)$".toRegex(
            RegexOption.DOT_MATCHES_ALL
        )
    private val atRegex = "(?<!Script)@(\\d+)".toRegex()
    private val numberRegex = "\\d+".toRegex()
    private val lettersRegex = "[a-zA-Z]+".toRegex()
    private val timeFormat = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        day()
        char(' ')
        hour()
        char(':')
        minute()
        char(':')
        second()
        char('.')
        secondFraction(3)
    }

    @OptIn(ExperimentalTime::class)
    private fun processLogFile(logPath: Path, uidMap: Map<String, Long>, botIdToSenderId: Map<String, String>) {
        logger.info { "Migrating: ${logPath.pathString}" }

        var readingMessage = false
        val sb = StringBuilder()

        var timestamp = Instant.DISTANT_PAST
        var botId = ""
        var isBot = false
        var platform = ""
        var globalSubjectId: String? = null
        var globalSubjectName: String? = null
        var subjectId: String? = null
        var subjectName: String? = null
        var senderId = ""
        var senderUid: Long = -1
        var senderName = ""

        fun loadChannelMessageMetadata(values: List<String>) {
            globalSubjectName = values[4]
            globalSubjectId = values[5]
            subjectName = values[6]
            subjectId = values[7]
        }

        fun loadGroupMessageMetadata(values: List<String>) {
            globalSubjectName = values[4]
            globalSubjectId = values[5]
            subjectName = globalSubjectName
            subjectId = globalSubjectId
        }

        fun loadMemberPrivateMessageMetadata(values: List<String>) {
            globalSubjectName = values[4]
            globalSubjectId = values[5]
            subjectName = values[9]
            subjectId = values[10]
        }

        fun loadContactMessageMetadata(values: List<String>) {
            globalSubjectName = null
            globalSubjectId = null
            subjectName = values[9]
            subjectId = values[10]
        }

        fun loadUnknownMessageMetadata(values: List<String>) {
            isBot = true // since all unknown messages are sent by bot itself
            globalSubjectName = "unknown"
            globalSubjectId = "unknown"
            subjectName = "unknown"
            subjectId = "unknown"
        }

        fun loadMessageMetadata(values: List<String>) {
            timestamp = LocalDateTime.parse(values[1], timeFormat).toInstant()
            botId = values[2]
            isBot = values[3] == "tx"
            platform = if (numberRegex.matches(botId)) "onebot" else "kook"

            when {
                (4..7).all { values[it].isNotEmpty() } -> loadChannelMessageMetadata(values)
                (4..5).all { values[it].isNotEmpty() } && values[8] == "private" -> loadMemberPrivateMessageMetadata(
                    values
                )

                (4..5).all { values[it].isNotEmpty() } -> loadGroupMessageMetadata(values)
                (9..10).all { values[it].isNotEmpty() } && values[8] == "contact" -> loadContactMessageMetadata(values)
                values[8] == "unknown" -> loadUnknownMessageMetadata(values)
                else -> error("Cannot determine message type from log values: $values")
            }

            if (isBot) {
                senderName = "unknown"
                senderId = botIdToSenderId[botId] ?: error("Cannot find senderId for botId: $botId")
            } else {
                senderName = values[9]
                senderId = values[10]
            }
            senderUid = uidMap["$platform-$senderId"] ?: error("Cannot find UID for senderId: $senderId")

            if (senderId in botIdToSenderId.values) {
                isBot = true
            }
        }

        fun consumeMessage() {
            if (!readingMessage) {
                return
            }

            val finalMessage = sb.toString()
                .let {
                    var message = it

                    while (true) {
                        val index = message.indexOf("DefaultOneBotMessageSegmentElement")
                        if (index < 0) {
                            break
                        }
                        var nextIndex = message.indexOf(
                            "DefaultOneBotMessageSegmentElement",
                            index + "DefaultOneBotMessageSegmentElement".length
                        )
                        if (nextIndex < 0) {
                            nextIndex = message.length
                        }

                        val element = message.substring(index, nextIndex)
                        val rightBracketIndex = element.lastIndexOf(')')
                        if (rightBracketIndex < 0) {
                            error("Cannot parse DefaultOneBotMessageSegmentElement: $it")
                        }
                        val segment =
                            element.substring("DefaultOneBotMessageSegmentElement(segment=".length..<rightBracketIndex)
                                .replace("@[a-z0-9]+".toRegex(), "")

                        message =
                            message.substring(0..<index) +
                                    "[obs:raw:${segment.escapeMessageFormatIdentifiers()}]" +
                                    message.substring(index + rightBracketIndex + 1)
                    }

                    message
                }.splitByUnescapedPaired('[', ']')
                .map {
                    if (!it.startsWith("[")) {
                        return@map it
                    }

                    val segments = it
                        .drop(1)
                        .dropLast(1)
                        .splitUnescaped(':')

                    if (segments.isEmpty()) {
                        return@map it
                    }

                    if (lettersRegex.matches(segments[0])) {
                        return@map it
                    }

                    val finalSegments = when (segments[0]) {
                        "图片" -> listOf("image", "unknown")
                        "表情" -> listOf("face", segments.getOrNull(1) ?: "unknown")
                        else -> run {
                            logger.warn { "Unknown message type: $it" }
                            segments
                        }
                    }

                    finalSegments.joinToString(":", "[", "]")
                }.joinToString("")
                .replace(atRegex, "[at:user:$1]")
                .replace("\\\\(?![\\[\\]:])".toRegex(), "\\\\")
                .replace(",", "\\,")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
            sb.clear()

            MessageHistory.new {
                this.platform = platform
                this.botId = botId

                this.globalSubjectId = globalSubjectId
                this.globalSubjectName = globalSubjectName

                this.subjectId = subjectId
                this.subjectName = subjectName

                this.senderId = senderId
                this.senderUid = senderUid
                this.senderName = senderName

                this.messageId = null
                this.message = finalMessage

                this.time = timestamp

                this.isBot = isBot

                this.version = 2
            }

            readingMessage = false
        }

        logPath.forEachLine {
            if (logStartRegex.containsMatchIn(it)) {
                consumeMessage()

                if (!messageStartRegex.containsMatchIn(it)) {
                    readingMessage = false
                    return@forEachLine
                }

                val match = messageRegex.matchAt(it, 0) ?: error("Cannot parse message log line: $it")

                readingMessage = true
                loadMessageMetadata(match.groupValues)
                sb.append(match.groupValues[11])
                return@forEachLine
            }

            if (!readingMessage) {
                return@forEachLine
            }

            sb.append('\n')
            sb.append(it)
        }
    }

    @OptIn(ExperimentalPathApi::class)
    suspend fun migrateConsoleLogMessageHistory(botIdToSenderId: Map<String, String>) {
        val uidMap = UIDManager.getAll()

        val logPath = Path("logs")
        val tempLogPath = Path("data/temp/logs")
        tempLogPath.toFile().deleteOnExit()

        logger.info { "Copying logs to temp folder..." }
        logPath.copyToRecursively(
            tempLogPath.createParentDirectories(),
            { _, _, e -> throw e },
            followLinks = true,
            overwrite = true
        )

        tempLogPath.walk(PathWalkOption.FOLLOW_LINKS).forEach {
            transaction(DatabaseManager.mainDatabase) {
                processLogFile(it, uidMap, botIdToSenderId)
            }
        }

        tempLogPath.deleteRecursively()
    }
}