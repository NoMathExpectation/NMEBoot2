package NoMathExpectation.NMEBoot.command.impl.source

import NoMathExpectation.NMEBoot.message.element.Attachment
import NoMathExpectation.NMEBoot.message.element.OneBotIncomingAttachment
import NoMathExpectation.NMEBoot.message.element.deleteAfterDelay
import NoMathExpectation.NMEBoot.message.onebot.OneBotFileCache
import NoMathExpectation.NMEBoot.message.onebot.OneBotFolding
import NoMathExpectation.NMEBoot.message.onebot.apiExt.toOneBotUploadApi
import NoMathExpectation.NMEBoot.message.onebot.containsOneBotForward
import NoMathExpectation.NMEBoot.user.idToUid
import NoMathExpectation.NMEBoot.util.asMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.common.id.toLong
import love.forte.simbot.common.id.toLongID
import love.forte.simbot.component.onebot.v11.core.actor.OneBotFriend
import love.forte.simbot.component.onebot.v11.core.actor.OneBotGroup
import love.forte.simbot.component.onebot.v11.core.actor.OneBotMember
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
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
    sendBlock: (Message) -> MessageReceipt,
): MessageReceipt {
    require(subject is OneBotGroup || subject is OneBotFriend || subject is OneBotMember) {
        "subject must be OneBotGroup, OneBotFriend or OneBotMember"
    }

    var finalMessage = this

    if (subject is OneBotGroup) {
        finalMessage
            .asMessages()
            .filterIsInstance<Attachment>()
            .forEach {
                val api = it.toOneBotUploadApi(subject.id.toLongID())
                val result = bot.executeResult(api)
                if (!result.isSuccess) {
                    throw RuntimeException("上传文件 ${it.name} 失败")
                }

                OneBotFileCache[subject.id.toLong(), bot.id.toLong(), it.name]?.let {
                    OneBotIncomingAttachment(bot, subject.id.toLongID(), it).deleteAfterDelay(1000 * 60 * 10)
                } ?: logger.warn { "Unable to find file uploaded by bot in cache with name ${it.name}." }
            }
        finalMessage = finalMessage.asMessages().filter { it !is Attachment }.toMessages()
    } else {
        finalMessage = finalMessage
            .asMessages()
            .map {
                if (it is Attachment) {
                    "文件 ${it.name}".toText()
                } else {
                    it
                }
            }.toMessages()
    }

    if (finalMessage.isEmpty()) {
        return NoDeleteOpMessageReceipt
    }

    if (subject is OneBotGroup) {
        val foldResult = OneBotFolding.processGroupFold(bot, finalMessage, subject)
        finalMessage = foldResult.first ?: finalMessage

        foldResult.second?.let { return it }
    }

    return sendBlock(finalMessage)
}

interface OneBotGroupMemberCommandSource<out T> : OneBotCommandSource<T>, ChatGroupMemberCommandSource<T> {
    override val globalSubject get() = subject
    override val subject: OneBotGroup
    override val executor: OneBotMember

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
            return message.sendByOneBot(bot, globalSubject) { subject.send(it) }
        }

        override suspend fun reply(message: Message): MessageReceipt {
            return message.sendByOneBot(bot, globalSubject) {
                if (it.containsOneBotForward()) {
                    return subject.send(it)
                }

                return origin.reply(it)
            }
        }

        companion object {
            suspend operator fun invoke(origin: OneBotNormalGroupMessageEvent) = NormalEvent(origin).apply { init() }
        }
    }
}

interface OneBotGroupMemberPrivateCommandSource<out T> : OneBotCommandSource<T>, MemberPrivateCommandSource<T> {
    override val globalSubject: OneBotGroup
    override val subject: OneBotMember
    override val executor: OneBotMember get() = subject

    override val permissionIds: List<String>
        get() = listOf(primaryPermissionId, id, "$platform-group-${subject.id}-private", platform)

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
            return message.sendByOneBot(bot, globalSubject) { subject.send(it) }
        }

        override suspend fun reply(message: Message): MessageReceipt {
            return message.sendByOneBot(bot, globalSubject) {
                if (it.containsOneBotForward()) {
                    return subject.send(it)
                }

                return origin.reply(it)
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
}

interface OneBotFriendCommandSource<out T> : OneBotCommandSource<T>, ContactCommandSource<T> {
    override val subject get() = executor
    override val executor: OneBotFriend

    override val permissionIds: List<String>
        get() = listOf(primaryPermissionId, id, "$platform-friend-${subject.id}", platform)

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
            return message.sendByOneBot(bot, subject) { subject.send(it) }
        }

        override suspend fun reply(message: Message): MessageReceipt {
            return message.sendByOneBot(bot, subject) {
                if (it.containsOneBotForward()) {
                    return subject.send(it)
                }

                return origin.reply(it)
            }
        }

        companion object {
            suspend operator fun invoke(origin: OneBotFriendMessageEvent) = Event(origin).apply { init() }
        }
    }
}