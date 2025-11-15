package NoMathExpectation.NMEBoot.command.impl.source

import NoMathExpectation.NMEBoot.bot.simbotApplication
import NoMathExpectation.NMEBoot.command.impl.source.offline.OfflineOneBotFriendCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.offline.OfflineOneBotGroupMemberCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.offline.OfflineOneBotGroupMemberPrivateCommandSource
import NoMathExpectation.NMEBoot.message.aggregateAll
import NoMathExpectation.NMEBoot.message.element.Attachment
import NoMathExpectation.NMEBoot.message.element.OneBotIncomingAttachment
import NoMathExpectation.NMEBoot.message.element.asMessageReceipt
import NoMathExpectation.NMEBoot.message.element.deleteAfterDelay
import NoMathExpectation.NMEBoot.message.onebot.OneBotFileCache
import NoMathExpectation.NMEBoot.message.onebot.OneBotFolding
import NoMathExpectation.NMEBoot.message.onebot.apiExt.toOneBotGroupUploadApi
import NoMathExpectation.NMEBoot.message.onebot.containsOneBotForward
import NoMathExpectation.NMEBoot.user.idToUid
import NoMathExpectation.NMEBoot.util.asMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.bot.find
import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.toLong
import love.forte.simbot.common.id.toLongID
import love.forte.simbot.component.onebot.v11.core.actor.OneBotFriend
import love.forte.simbot.component.onebot.v11.core.actor.OneBotGroup
import love.forte.simbot.component.onebot.v11.core.actor.OneBotMember
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBotManager
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotFriendMessageEvent
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotGroupPrivateMessageEvent
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotNormalGroupMessageEvent
import love.forte.simbot.definition.Actor
import love.forte.simbot.definition.User
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageReceipt
import love.forte.simbot.message.toMessages
import love.forte.simbot.message.toText

interface OneBotCommandSource<out T> : CommandSource<T>, BotAwareCommandSource<T> {
    override val bot: OneBotBot
    override val subject: Actor
    override val executor: User

    override val id: String
        get() = "$platform-${executor.id}"

    override val platform: String
        get() = "onebot"
}

private val logger = KotlinLogging.logger { }

private suspend inline fun Message.sendByOneBot(
    bot: OneBotBot,
    subject: Actor,
    crossinline sendBlock: suspend (Message) -> MessageReceipt,
): MessageReceipt {
    require(subject is OneBotGroup || subject is OneBotFriend || subject is OneBotMember) {
        "subject must be OneBotGroup, OneBotFriend or OneBotMember"
    }

    var finalMessage = this
    val receipts = mutableListOf<MessageReceipt>()

    if (subject is OneBotGroup) {
        finalMessage
            .asMessages()
            .filterIsInstance<Attachment>()
            .forEach {
                val api = it.toOneBotGroupUploadApi(subject.id.toLongID())
                val result = bot.executeResult(api)
                if (!result.isSuccess) {
                    throw RuntimeException("上传文件 ${it.name} 失败")
                }

                OneBotFileCache[subject.id.toLong(), bot.id.toLong(), it.name]?.let {
                    val attachment = OneBotIncomingAttachment(bot, subject.id.toLongID(), it)
                    attachment.deleteAfterDelay(bot, 1000 * 60 * 10)
                    receipts += attachment.asMessageReceipt()
                } ?: logger.warn { "Unable to find file uploaded by bot in cache with name ${it.name}." }
            }
        finalMessage = finalMessage.asMessages().filter { it !is Attachment }.toMessages()
    } else {
        finalMessage = finalMessage
            .asMessages()
            .map {
                if (it is Attachment) {
                    "文件 ${it.name}\n".toText()
                } else {
                    it
                }
            }.toMessages()
    }

    if (finalMessage.isEmpty()) {
        return receipts.aggregateAll()
    }

    if (subject is OneBotGroup) {
        val foldResult = OneBotFolding.processGroupFold(bot, finalMessage, subject)
        finalMessage = foldResult.first ?: finalMessage

        foldResult.second?.let {
            receipts += it
            return receipts.aggregateAll()
        }
    }

    receipts += sendBlock(finalMessage)

    return receipts.aggregateAll()
}

interface OneBotGroupMemberCommandSource<out T> : OneBotCommandSource<T>, ChatGroupMemberCommandSource<T> {
    override val globalSubject get() = subject
    override val subject: OneBotGroup
    override val executor: OneBotMember

    override suspend fun isBotModerator() = hasPermission(adminPermission) || executor.role?.isAdmin == true

    override suspend fun toOffline() = OfflineOneBotGroupMemberCommandSource(bot.id, subject.id, executor.id)

    class NormalEvent private constructor(override val origin: OneBotNormalGroupMessageEvent) :
        OneBotGroupMemberCommandSource<OneBotNormalGroupMessageEvent> {
        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override val bot = origin.bot

        private var _subject: OneBotGroup? = null
        override val subject: OneBotGroup
            get() = _subject ?: error("subject not initialized!")

        private var _executor: OneBotMember? = null
        override val executor: OneBotMember
            get() = _executor ?: error("executor not initialized!")

        suspend fun init() {
            _subject = origin.content()
            _executor = origin.author()
            _uid = idToUid()
        }

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, globalSubject) { subject.send(it) }
            }
        }

        override suspend fun reply(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, globalSubject) {
                    if (it.containsOneBotForward()) {
                        subject.send(it)
                    } else {
                        origin.reply(it)
                    }
                }
            }
        }

        companion object {
            suspend operator fun invoke(origin: OneBotNormalGroupMessageEvent) = NormalEvent(origin).apply { init() }
        }
    }

    class Data private constructor(
        private val botId: ID,
        private val groupId: ID,
        private val memberId: ID,
    ) : OneBotGroupMemberCommandSource<OneBotMember> {
        override val origin: OneBotMember
            get() = executor

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override val bot by lazy {
            simbotApplication
                ?.botManagers
                ?.find<OneBotBotManager>()
                ?.find(botId)
                ?: error("OneBotBot $botId not found!")
        }

        private var _subject: OneBotGroup? = null
        override val subject: OneBotGroup
            get() = _subject ?: error("subject not initialized!")

        private var _executor: OneBotMember? = null
        override val executor: OneBotMember
            get() = _executor ?: error("executor not initialized!")

        suspend fun init() {
            _subject = bot.groupRelation.group(groupId) ?: error("OneBotGroup $groupId not found!")
            _executor = subject.member(memberId) ?: error("OneBotMember $memberId not found!")
            _uid = idToUid()
        }

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, globalSubject) { subject.send(it) }
            }
        }

        override suspend fun reply(message: Message) = send(message)

        companion object {
            suspend operator fun invoke(botId: ID, groupId: ID, memberId: ID) =
                Data(botId, groupId, memberId).apply { init() }
        }
    }
}

interface OneBotGroupMemberPrivateCommandSource<out T> : OneBotCommandSource<T>, MemberPrivateCommandSource<T> {
    override val globalSubject: OneBotGroup
    override val globalSubjectPermissionId get() = "$platform-group-${subject.id}"
    override val subject: OneBotMember
    override val executor: OneBotMember get() = subject

    override val permissionIds: List<String>
        get() = listOf(primaryPermissionId, id, subjectPermissionId, globalSubjectPermissionId, platform)

    override suspend fun isBotModerator() = hasPermission(adminPermission) || executor.role?.isAdmin == true

    override suspend fun toOffline() =
        OfflineOneBotGroupMemberPrivateCommandSource(bot.id, globalSubject.id, executor.id)

    class Event private constructor(override val origin: OneBotGroupPrivateMessageEvent) :
        OneBotGroupMemberPrivateCommandSource<OneBotGroupPrivateMessageEvent> {
        private var _globalSubject: OneBotGroup? = null
        override val globalSubject: OneBotGroup
            get() = _globalSubject ?: error("globalSubject not initialized!")

        override val bot = origin.bot

        private var _subject: OneBotMember? = null
        override val subject: OneBotMember
            get() = _subject ?: error("subject not initialized!")

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, globalSubject) { subject.send(it) }
            }
        }

        override suspend fun reply(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, globalSubject) {
                    if (it.containsOneBotForward()) {
                        subject.send(it)
                    } else {
                        origin.reply(it)
                    }
                }
            }
        }

        suspend fun init() {
            _globalSubject = origin.source()
            _subject = origin.content()
            _uid = idToUid()
        }

        companion object {
            suspend operator fun invoke(origin: OneBotGroupPrivateMessageEvent) = Event(origin).apply { init() }
        }
    }

    class Data private constructor(
        private val botId: ID,
        private val groupId: ID,
        private val memberId: ID,
    ) : OneBotGroupMemberPrivateCommandSource<OneBotMember> {
        override val origin: OneBotMember
            get() = executor

        override val bot by lazy {
            simbotApplication
                ?.botManagers
                ?.find<OneBotBotManager>()
                ?.find(botId)
                ?: error("OneBotBot $botId not found!")
        }

        private var _globalSubject: OneBotGroup? = null
        override val globalSubject: OneBotGroup
            get() = _globalSubject ?: error("globalSubject not initialized!")

        private var _subject: OneBotMember? = null
        override val subject: OneBotMember
            get() = _subject ?: error("subject not initialized!")

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, globalSubject) { subject.send(it) }
            }
        }

        override suspend fun reply(message: Message) = send(message)

        suspend fun init() {
            _globalSubject = bot.groupRelation.group(groupId) ?: error("OneBotGroup $groupId not found!")
            _subject = globalSubject.member(memberId) ?: error("OneBotMember $memberId not found!")
            _uid = idToUid()
        }

        companion object {
            suspend operator fun invoke(botId: ID, groupId: ID, memberId: ID) =
                Data(botId, groupId, memberId).apply { init() }
        }
    }
}

interface OneBotFriendCommandSource<out T> : OneBotCommandSource<T>, ContactCommandSource<T> {
    override val subject get() = executor
    override val subjectPermissionId get() = "$platform-friend-${subject.id}"
    override val executor: OneBotFriend

    override val permissionIds: List<String>
        get() = listOf(primaryPermissionId, id, subjectPermissionId, platform)

    override suspend fun toOffline() = OfflineOneBotFriendCommandSource(bot.id, executor.id)

    class Event private constructor(override val origin: OneBotFriendMessageEvent) :
        OneBotFriendCommandSource<OneBotFriendMessageEvent> {
        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override val bot = origin.bot

        private var _executor: OneBotFriend? = null
        override val executor: OneBotFriend
            get() = _executor ?: error("executor not initialized!")

        suspend fun init() {
            _executor = origin.content()
            _uid = idToUid()
        }

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, subject) { subject.send(it) }
            }
        }

        override suspend fun reply(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, subject) {
                    if (it.containsOneBotForward()) {
                        subject.send(it)
                    } else {
                        origin.reply(it)
                    }
                }
            }
        }

        companion object {
            suspend operator fun invoke(origin: OneBotFriendMessageEvent) = Event(origin).apply { init() }
        }
    }

    class Data private constructor(
        private val botId: ID,
        private val friendId: ID,
    ) : OneBotFriendCommandSource<OneBotFriend> {
        override val origin: OneBotFriend
            get() = executor

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override val bot by lazy {
            simbotApplication
                ?.botManagers
                ?.find<OneBotBotManager>()
                ?.find(botId)
                ?: error("OneBotBot $botId not found!")
        }

        private var _executor: OneBotFriend? = null
        override val executor: OneBotFriend
            get() = _executor ?: error("executor not initialized!")

        suspend fun init() {
            _executor = bot.contactRelation.contact(friendId) ?: error("OneBotFriend $friendId not found!")
            _uid = idToUid()
        }

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) { finalMessage ->
                finalMessage.sendByOneBot(bot, subject) { subject.send(it) }
            }
        }

        override suspend fun reply(message: Message) = send(message)

        companion object {
            suspend operator fun invoke(botId: ID, friendId: ID) = Data(botId, friendId).apply { init() }
        }
    }
}