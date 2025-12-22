package NoMathExpectation.NMEBoot.command.impl.source

import NoMathExpectation.NMEBoot.bot.simbotApplication
import NoMathExpectation.NMEBoot.command.impl.source.offline.OfflineKookChannelCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.offline.OfflineKookPrivateCommandSource
import NoMathExpectation.NMEBoot.message.element.Attachment
import NoMathExpectation.NMEBoot.user.idToUid
import NoMathExpectation.NMEBoot.util.asMessages
import io.ktor.client.request.forms.*
import io.ktor.utils.io.jvm.javaio.*
import love.forte.simbot.ability.SendSupport
import love.forte.simbot.annotations.ExperimentalSimbotAPI
import love.forte.simbot.bot.find
import love.forte.simbot.common.collectable.toList
import love.forte.simbot.common.id.ID
import love.forte.simbot.component.kook.*
import love.forte.simbot.component.kook.bot.KookBot
import love.forte.simbot.component.kook.bot.KookBotManager
import love.forte.simbot.component.kook.event.KookChannelMessageEvent
import love.forte.simbot.component.kook.event.KookContactMessageEvent
import love.forte.simbot.component.kook.role.KookMemberRole
import love.forte.simbot.definition.Actor
import love.forte.simbot.definition.User
import love.forte.simbot.kook.api.asset.CreateAssetApi
import love.forte.simbot.kook.messages.MessageType
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageReceipt
import love.forte.simbot.message.buildMessages
import love.forte.simbot.message.toMessages

interface KookCommandSource<out T> : CommandSource<T>, BotAwareCommandSource<T> {
    override val bot: KookBot
    override val subject: Actor
    override val executor: User

    override val id: String
        get() = "$platform-${executor.id}"

    override val platform: String
        get() = "kook"
}

private suspend fun KookCommandSource<*>.processMessage(message: Message): Message {
    var finalMessage = message

    val assets = finalMessage
        .asMessages()
        .filterIsInstance<Attachment>()
        .map {
            val channel = it.inputStream().toByteReadChannel()
            val api = CreateAssetApi.create(ChannelProvider { channel }, it.name)
            bot.uploadAsset(api, MessageType.FILE)
        }
    finalMessage = finalMessage.asMessages().filter { it !is Attachment }.toMessages()
    if (assets.isNotEmpty()) {
        finalMessage = buildMessages {
            assets.forEach { +it }
            +finalMessage.asMessages()
        }
    }

    return finalMessage
}

interface KookChannelCommandSource<out T> : KookCommandSource<T>, GuildMemberCommandSource<T> {
    override val globalSubject: KookGuild
    override val subject: KookChannel
    override val executor: KookMember

    @OptIn(ExperimentalSimbotAPI::class)
    override val roles: List<KookMemberRole>
        get() = executor.roles.toList()

    override suspend fun botAsSource(): KookChannelCommandSource<*> {
        return Data.invoke(bot.id, globalSubject.id, subject.id, bot.id)
    }

    override suspend fun isBotModerator() = hasPermission(adminPermission) || globalSubject.owner().id == executor.id

    override suspend fun toOffline() = OfflineKookChannelCommandSource(
        bot.id,
        globalSubject.id,
        subject.id,
        executor.id
    )

    class Event private constructor(override val origin: KookChannelMessageEvent) :
        KookChannelCommandSource<KookChannelMessageEvent> {
        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override val bot = origin.bot

        private var _globalSubject: KookGuild? = null
        override val globalSubject: KookGuild
            get() = _globalSubject ?: error("globalSubject not initialized!")

        private var _subject: KookChatChannel? = null
        override val subject: KookChatChannel
            get() = _subject ?: error("subject not initialized!")

        private var _executor: KookMember? = null
        override val executor: KookMember
            get() = _executor ?: error("executor not initialized!")

        private suspend fun init() {
            _globalSubject = origin.source()
            _subject = origin.content()
            _executor = origin.author()
            _uid = idToUid()
        }

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) {
                val finalMessage = processMessage(it)
                subject.send(finalMessage)
            }
        }

        override suspend fun reply(message: Message): MessageReceipt {
            return sendAndBroadcast(message) {
                val finalMessage = processMessage(it)
                origin.reply(finalMessage)
            }
        }

        companion object {
            suspend operator fun invoke(event: KookChannelMessageEvent): Event {
                return Event(event).apply { init() }
            }
        }
    }

    class Data private constructor(
        private val botId: ID,
        private val guildId: ID,
        private val channelId: ID,
        private val memberId: ID,
    ) : KookChannelCommandSource<KookMember> {
        override val origin get() = executor

        override val bot by lazy {
            simbotApplication
                ?.botManagers
                ?.find<KookBotManager>()
                ?.find(botId) as? KookBot
                ?: error("KookBot $botId not found!")
        }

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        private var _globalSubject: KookGuild? = null
        override val globalSubject: KookGuild
            get() = _globalSubject ?: error("globalSubject not initialized!")

        private var _subject: KookChannel? = null
        override val subject: KookChannel
            get() = _subject ?: error("subject not initialized!")

        private var _executor: KookMember? = null
        override val executor: KookMember
            get() = _executor ?: error("executor not initialized!")

        private suspend fun init() {
            _globalSubject = bot.guildRelation.guild(guildId) ?: error("Guild $guildId not found!")
            _subject = globalSubject.channel(channelId) ?: error("Channel $channelId not found!")
            _executor = globalSubject.member(memberId) ?: error("Member $memberId not found!")
            _uid = idToUid()
        }

        override suspend fun send(message: Message): MessageReceipt {
            val subject = subject
            if (subject !is SendSupport) {
                throw UnsupportedOperationException("Channel $channelId does not support sending messages!")
            }

            return sendAndBroadcast(message) {
                val finalMessage = processMessage(it)
                subject.send(finalMessage)
            }
        }

        override suspend fun reply(message: Message) = send(message)

        companion object {
            suspend operator fun invoke(
                botId: ID,
                guildId: ID,
                channelId: ID,
                memberId: ID,
            ): Data {
                return Data(botId, guildId, channelId, memberId).apply { init() }
            }
        }
    }
}

interface KookPrivateCommandSource<out T> : KookCommandSource<T>, ContactCommandSource<T> {
    override val globalSubject get() = null
    override val subject: KookUserChat get() = executor
    override val executor: KookUserChat

    override suspend fun botAsSource(): KookPrivateCommandSource<*> {
        return Data.invoke(bot.id, bot.id)
    }

    override suspend fun toOffline() = OfflineKookPrivateCommandSource(bot.id, executor.id)

    class Event private constructor(override val origin: KookContactMessageEvent) :
        KookPrivateCommandSource<KookContactMessageEvent> {
        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override val bot = origin.bot

        private var _executor: KookUserChat? = null
        override val executor: KookUserChat
            get() = _executor ?: error("executor not initialized!")

        private suspend fun init() {
            _executor = origin.content()
            _uid = idToUid()
        }

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) {
                val finalMessage = processMessage(it)
                executor.send(finalMessage)
            }
        }

        override suspend fun reply(message: Message): MessageReceipt {
            return sendAndBroadcast(message) {
                val finalMessage = processMessage(it)
                origin.reply(finalMessage)
            }
        }

        companion object {
            suspend operator fun invoke(event: KookContactMessageEvent): Event {
                return Event(event).apply { init() }
            }
        }
    }

    class Data private constructor(
        private val botId: ID,
        private val userId: ID,
    ) : KookPrivateCommandSource<KookUserChat> {
        override val origin get() = executor

        override val bot by lazy {
            simbotApplication
                ?.botManagers
                ?.firstBot(botId) as? KookBot
                ?: error("KookBot $botId not found!")
        }

        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        private var _executor: KookUserChat? = null
        override val executor: KookUserChat
            get() = _executor ?: error("executor not initialized!")

        private suspend fun init() {
            _executor = bot.contactRelation.contact(userId)
            _uid = idToUid()
        }

        override suspend fun send(message: Message): MessageReceipt {
            return sendAndBroadcast(message) {
                val finalMessage = processMessage(it)
                executor.send(finalMessage)
            }
        }

        override suspend fun reply(message: Message) = send(message)

        companion object {
            suspend operator fun invoke(botId: ID, userId: ID): Data {
                return Data(botId, userId).apply { init() }
            }
        }
    }
}