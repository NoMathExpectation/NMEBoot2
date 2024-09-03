package NoMathExpectation.NMEBoot.command.impl.source

import NoMathExpectation.NMEBoot.message.element.Attachment
import NoMathExpectation.NMEBoot.user.idToUid
import NoMathExpectation.NMEBoot.util.asMessages
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import love.forte.simbot.annotations.ExperimentalSimbotAPI
import love.forte.simbot.common.collectable.toList
import love.forte.simbot.component.kook.*
import love.forte.simbot.component.kook.bot.KookBot
import love.forte.simbot.component.kook.event.KookChannelMessageEvent
import love.forte.simbot.component.kook.event.KookContactMessageEvent
import love.forte.simbot.component.kook.role.KookMemberRole
import love.forte.simbot.definition.Actor
import love.forte.simbot.definition.User
import love.forte.simbot.kook.api.asset.CreateAssetApi
import love.forte.simbot.kook.messages.MessageType
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageContent
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

interface KookChannelCommandSource<out T> : KookCommandSource<T>, GuildMemberCommandSource<T> {
    override val globalSubject: KookGuild
    override val subject: KookChannel
    override val executor: KookMember

    @OptIn(ExperimentalSimbotAPI::class)
    override val roles: List<KookMemberRole>
        get() = executor.roles.toList()

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

        suspend fun init() {
            _globalSubject = origin.source()
            _subject = origin.content()
            _executor = origin.author()
            _uid = idToUid()
        }

        private suspend fun processMessage(message: Message): Message {
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
                    +finalMessage
                }
            }

            return finalMessage
        }

        @OptIn(ExperimentalSimbotAPI::class)
        override suspend fun send(message: Message): MessageReceipt {
            val finalMessage = processMessage(message)
            return subject.send(finalMessage)
        }

        override suspend fun reply(message: Message): MessageReceipt {
            val finalMessage = processMessage(message)
            return origin.reply(finalMessage)
        }

        companion object {
            suspend operator fun invoke(event: KookChannelMessageEvent): Event {
                return Event(event).apply { init() }
            }
        }
    }
}

interface KookPrivateCommandSource<out T> : KookCommandSource<T>, ContactCommandSource<T> {
    override val globalSubject get() = null
    override val subject: KookUserChat get() = executor
    override val executor: KookUserChat

    class Event private constructor(override val origin: KookContactMessageEvent) :
        KookPrivateCommandSource<KookContactMessageEvent> {
        private var _uid: Long? = null
        override val uid: Long
            get() = _uid ?: error("uid not initialized!")

        override val bot = origin.bot

        private var _executor: KookUserChat? = null
        override val executor: KookUserChat
            get() = _executor ?: error("executor not initialized!")

        suspend fun init() {
            _executor = origin.content()
            _uid = idToUid()
        }

        override suspend fun send(message: Message) = executor.send(message)

        override suspend fun send(text: String) = executor.send(text)

        override suspend fun send(messageContent: MessageContent) = executor.send(messageContent)

        override suspend fun reply(message: Message) = origin.reply(message)

        override suspend fun reply(text: String) = origin.reply(text)

        override suspend fun reply(messageContent: MessageContent) = origin.reply(messageContent)

        companion object {
            suspend operator fun invoke(event: KookContactMessageEvent): Event {
                return Event(event).apply { init() }
            }
        }
    }
}