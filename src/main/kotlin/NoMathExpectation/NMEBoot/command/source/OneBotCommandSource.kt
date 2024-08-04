package NoMathExpectation.NMEBoot.command.source

import NoMathExpectation.NMEBoot.message.OneBotFolding
import NoMathExpectation.NMEBoot.user.idToUid
import NoMathExpectation.NMEBoot.util.nickOrName
import love.forte.simbot.component.onebot.v11.core.actor.OneBotFriend
import love.forte.simbot.component.onebot.v11.core.actor.OneBotGroup
import love.forte.simbot.component.onebot.v11.core.actor.OneBotMember
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotFriendMessageEvent
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotGroupPrivateMessageEvent
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotNormalGroupMessageEvent
import love.forte.simbot.component.onebot.v11.message.OneBotMessageReceipt
import love.forte.simbot.definition.Actor
import love.forte.simbot.definition.User
import love.forte.simbot.message.Message

interface OneBotCommandSource<out T> : CommandSource<T> {
    override val subject: Actor
    override val executor: User

    override val id: String
        get() = "$platform-${executor.id}"

    override val platform: String
        get() = "onebot"
}

interface OneBotGroupMemberCommandSource<out T> : OneBotCommandSource<T>, ChatGroupMemberCommandSource<T> {
    override val globalSubject get() = subject
    override val subject: OneBotGroup
    override val executor: OneBotMember

    class NormalEvent private constructor(override val origin: OneBotNormalGroupMessageEvent) :
        OneBotGroupMemberCommandSource<OneBotNormalGroupMessageEvent> {
        private val bot = origin.bot

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

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

        override suspend fun sendRaw(message: Message): OneBotMessageReceipt {
            val finalMessage = OneBotFolding.processFold(bot, message, subject.botAsMember().nickOrName)
            return subject.send(finalMessage)
        }

        override suspend fun replyRaw(message: Message): OneBotMessageReceipt {
            val finalMessage = OneBotFolding.processFold(bot, message, subject.botAsMember().nickOrName)
            return origin.reply(finalMessage)
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
        get() = listOf(uidToPermissionId, id, "$platform-group-${subject.id}-private", platform)

    class Event private constructor(override val origin: OneBotGroupPrivateMessageEvent) :
        OneBotGroupMemberPrivateCommandSource<OneBotGroupPrivateMessageEvent> {
        private val bot = origin.bot

        private var _globalSubject: OneBotGroup? = null
        override val globalSubject: OneBotGroup
            get() = _globalSubject ?: error("globalSubject not initialized!")

        private var _subject: OneBotMember? = null
        override val subject: OneBotMember
            get() = _subject ?: error("subject not initialized!")

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override suspend fun sendRaw(message: Message): OneBotMessageReceipt {
            val finalMessage = OneBotFolding.processFold(bot, message)
            return subject.send(finalMessage)
        }

        override suspend fun replyRaw(message: Message): OneBotMessageReceipt {
            val finalMessage = OneBotFolding.processFold(bot, message)
            return origin.reply(finalMessage)
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
        get() = listOf(uidToPermissionId, id, "$platform-friend-${subject.id}", platform)

    class Event private constructor(override val origin: OneBotFriendMessageEvent) :
        OneBotFriendCommandSource<OneBotFriendMessageEvent> {
        private val bot = origin.bot

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        private var _executor: OneBotFriend? = null
        override val executor: OneBotFriend
            get() = _executor ?: error("executor not initialized!")

        suspend fun init() {
            _executor = origin.content()
            _uid = idToUid()
        }

        override suspend fun sendRaw(message: Message): OneBotMessageReceipt {
            val finalMessage = OneBotFolding.processFold(bot, message)
            return subject.send(finalMessage)
        }

        override suspend fun replyRaw(message: Message): OneBotMessageReceipt {
            val finalMessage = OneBotFolding.processFold(bot, message)
            return origin.reply(finalMessage)
        }

        companion object {
            suspend operator fun invoke(origin: OneBotFriendMessageEvent) = Event(origin).apply { init() }
        }
    }
}