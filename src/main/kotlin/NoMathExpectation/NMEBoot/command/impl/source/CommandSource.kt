package NoMathExpectation.NMEBoot.command.impl.source

import NoMathExpectation.NMEBoot.command.impl.PermissionServiceAware
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.definition.*
import love.forte.simbot.message.Message
import love.forte.simbot.message.MessageContent
import love.forte.simbot.message.MessageReceipt
import love.forte.simbot.message.toText
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

interface CommandSource<out T> : PermissionServiceAware {
    val origin: T

    val uid: Long

    val id: String

    override val primaryPermissionId
        get() = "uid-$uid"

    override val permissionIds
        get() = listOf(primaryPermissionId, id, platform)

    val platform: String

    val globalSubject: Organization?

    val subject: Actor?

    val executor: User?

    suspend fun sendRaw(message: Message): MessageReceipt?

    suspend fun send(message: Message): MessageReceipt? {
        // val finalMessage = with(MessageProcessor) { processMessage(message) }
        return sendRaw(message)
    }

    suspend fun replyRaw(message: Message): MessageReceipt?

    suspend fun reply(message: Message): MessageReceipt? {
        // val finalMessage = with(MessageProcessor) { processMessage(message) }
        return replyRaw(message)
    }

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
            // onebot
            register(OneBotGroupMemberCommandSource.NormalEvent.Companion::invoke)
            register(OneBotGroupMemberPrivateCommandSource.Event.Companion::invoke)
            register(OneBotFriendCommandSource.Event.Companion::invoke)

            // kook
            register(KookChannelCommandSource.Event.Companion::invoke)
            register(KookPrivateCommandSource.Event.Companion::invoke)
        }
    }
}

suspend fun CommandSource<*>.sendRaw(text: String): MessageReceipt? = sendRaw(text.toText())

suspend fun CommandSource<*>.sendRaw(messageContent: MessageContent): MessageReceipt? = sendRaw(messageContent.messages)

suspend fun CommandSource<*>.send(text: String): MessageReceipt? = send(text.toText())

suspend fun CommandSource<*>.send(messageContent: MessageContent): MessageReceipt? = send(messageContent.messages)

suspend fun CommandSource<*>.replyRaw(text: String): MessageReceipt? = replyRaw(text.toText())

suspend fun CommandSource<*>.replyRaw(messageContent: MessageContent): MessageReceipt? =
    replyRaw(messageContent.messages)

suspend fun CommandSource<*>.reply(text: String): MessageReceipt? = reply(text.toText())

suspend fun CommandSource<*>.reply(messageContent: MessageContent): MessageReceipt? = reply(messageContent.messages)

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

    val roles: List<Role>

    override val permissionIds: List<String>
        get() = listOf(
            primaryPermissionId,
            id,
            *rolesToPermissionIds.toTypedArray(),
            "$platform-guild-${globalSubject.id}-${subject.id}",
            platform
        )
}

val GuildMemberCommandSource<*>.rolesToPermissionIds
    get() = roles.map {
        "$platform-guild-${globalSubject.id}-role-${it.id}"
    }

interface ChatGroupMemberCommandSource<out T> : MemberCommandSource<T> {
    override val globalSubject get() = subject
    override val subject: ChatGroup

    override val permissionIds: List<String>
        get() = listOf(primaryPermissionId, id, "$platform-group-${subject.id}", platform)
}

interface MemberPrivateCommandSource<out T> : MemberCommandSource<T> {
    override val subject get() = executor
}

interface ContactCommandSource<out T> : UserCommandSource<T> {
    override val globalSubject get() = null
    override val subject get() = executor
    override val executor: Contact

    override val permissionIds: List<String>
        get() = listOf(primaryPermissionId, id, "$platform-contact-${subject.id}", platform)
}