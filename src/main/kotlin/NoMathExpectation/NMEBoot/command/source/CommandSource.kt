package NoMathExpectation.NMEBoot.command.source

import NoMathExpectation.NMEBoot.command.util.PermissionAware
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.definition.*
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageContent
import love.forte.simbot.message.MessageReceipt
import love.forte.simbot.message.toText
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

interface CommandSource<out T> : PermissionAware {
    val origin: T

    val uid: Long

    val id: String

    val uidToPermissionId get() = "uid-$uid"

    override val permissionIds: List<String>
        get() = listOf(uidToPermissionId, id, platform)

    val platform: String

    val globalSubject: Organization?

    val subject: Actor?

    val executor: User?

    suspend fun sendRaw(text: String): MessageReceipt? = sendRaw(text.toText())

    suspend fun sendRaw(message: Message): MessageReceipt?

    suspend fun sendRaw(messageContent: MessageContent): MessageReceipt? = sendRaw(messageContent.messages)

    suspend fun send(text: String): MessageReceipt? = send(text.toText())

    suspend fun send(message: Message): MessageReceipt? {
        return sendRaw(message)
    }

    suspend fun send(messageContent: MessageContent): MessageReceipt? = send(messageContent.messages)

    suspend fun replyRaw(text: String): MessageReceipt? = replyRaw(text.toText())

    suspend fun replyRaw(message: Message): MessageReceipt?

    suspend fun replyRaw(messageContent: MessageContent): MessageReceipt? = replyRaw(messageContent.messages)

    suspend fun reply(text: String): MessageReceipt? = reply(text.toText())

    suspend fun reply(message: Message): MessageReceipt? {
        return replyRaw(message)
    }

    suspend fun reply(messageContent: MessageContent): MessageReceipt? = reply(messageContent.messages)

    companion object {
        fun interface CommandSourceBuilder<T : Any, R> {
            suspend fun build(origin: T): CommandSource<R>

            @Suppress("UNCHECKED_CAST")
            suspend fun buildFromAny(origin: Any) = (origin as? T)?.let { build(it) }
        }

        private val logger = KotlinLogging.logger { }

        private val registry: MutableMap<KClass<*>, CommandSourceBuilder<*, *>> = mutableMapOf()

        fun <T : Any, R> register(clazz: KClass<T>, builder: CommandSourceBuilder<T, R>) {
            registry[clazz] = builder

            logger.info { "Registered command source builder for type ${clazz.qualifiedName ?: "<unknown>"}" }
        }

        inline fun <reified T : Any, R> register(noinline builder: suspend (T) -> CommandSource<R>) {
            register(T::class, builder)
        }

        suspend fun get(origin: Any): CommandSource<*>? {
            logger.debug { "Attempting to resolve command source from $origin}" }

            var classes = listOf(origin::class)
            while (classes.isNotEmpty()) {
                classes.firstNotNullOfOrNull {
                    registry[it]?.buildFromAny(origin)
                }?.let {
                    logger.debug { "Resolved $origin as command source $it." }
                    return it
                }
                classes = classes.flatMap { it.superclasses }
            }

            logger.debug { "Resolved $origin as no command source." }
            return null
        }

        init {
            register(OneBotGroupMemberCommandSource.NormalEvent::invoke)
            register(OneBotGroupMemberPrivateCommandSource.Event::invoke)
            register(OneBotFriendCommandSource.Event::invoke)
        }
    }
}

interface UserCommandSource<out T> : CommandSource<T> {
    override val subject: Actor
    override val executor: User
}

interface MemberCommandSource<out T> : UserCommandSource<T> {
    override val globalSubject: Organization
    override val executor: Member
}

interface GuildMemberCommandSource<out T> : MemberCommandSource<T> {
    override val globalSubject: Guild
    override val subject: Channel

    override val permissionIds: List<String>
        get() = listOf(uidToPermissionId, id, "$platform-guild-${globalSubject.id}-${subject.id}", platform)
}

interface ChatGroupMemberCommandSource<out T> : MemberCommandSource<T> {
    override val globalSubject get() = subject
    override val subject: ChatGroup

    override val permissionIds: List<String>
        get() = listOf(uidToPermissionId, id, "$platform-group-${subject.id}", platform)
}

interface MemberPrivateCommandSource<out T> : MemberCommandSource<T> {
    override val subject get() = executor
}

interface ContactCommandSource<out T> : UserCommandSource<T> {
    override val globalSubject get() = null
    override val subject get() = executor
    override val executor: Contact

    override val permissionIds: List<String>
        get() = listOf(uidToPermissionId, id, "$platform-contact-${subject.id}", platform)
}