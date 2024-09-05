package NoMathExpectation.NMEBoot.command.impl.source

import NoMathExpectation.NMEBoot.command.impl.PermissionServiceAware
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.ability.DeleteOption
import love.forte.simbot.ability.ReplySupport
import love.forte.simbot.ability.SendSupport
import love.forte.simbot.ability.StandardDeleteOption
import love.forte.simbot.bot.Bot
import love.forte.simbot.common.id.IntID.Companion.ID
import love.forte.simbot.definition.*
import love.forte.simbot.message.*
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

interface CommandSource<out T> : PermissionServiceAware, SendSupport, ReplySupport {
    val origin: T

    val uid: Long

    val id: String

    override val primaryPermissionId
        get() = "uid-$uid"

    override val permissionIds
        get() = listOf(primaryPermissionId, id, platform)

    val platform: String

    val bot: Bot?

    val globalSubject: Organization?
    val globalSubjectPermissionId: String?

    val subject: Actor?
    val subjectPermissionId: String?

    val executor: User?

    override suspend fun send(message: Message): MessageReceipt

    override suspend fun send(text: String) = send(text.toText())

    override suspend fun send(messageContent: MessageContent) = send(messageContent.messages)

    override suspend fun reply(message: Message): MessageReceipt

    override suspend fun reply(text: String) = reply(text.toText())

    override suspend fun reply(messageContent: MessageContent) = reply(messageContent.messages)

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

interface BotAwareCommandSource<out T> : CommandSource<T> {
    override val bot: Bot
}

interface UserCommandSource<out T> : CommandSource<T>, BotAwareCommandSource<T> {
    override val subject: Actor
    override val subjectPermissionId: String
    override val executor: User
}

interface MemberCommandSource<out T> : UserCommandSource<T>, BotAwareCommandSource<T> {
    override val globalSubject: Organization
    override val globalSubjectPermissionId: String
    override val executor: Member
}

interface GuildMemberCommandSource<out T> : MemberCommandSource<T> {
    override val globalSubject: Guild
    override val globalSubjectPermissionId get() = "$platform-guild-${globalSubject.id}"
    override val subject: Channel
    override val subjectPermissionId get() = "$platform-guild-${globalSubject.id}-${subject.id}"

    val roles: List<Role>

    override val permissionIds: List<String>
        get() = listOf(
            primaryPermissionId,
            id,
            *rolesToPermissionIds.toTypedArray(),
            subjectPermissionId,
            globalSubjectPermissionId,
            platform
        )
}

val GuildMemberCommandSource<*>.rolesToPermissionIds
    get() = roles.map {
        "$platform-guild-${globalSubject.id}-role-${it.id}"
    }

interface ChatGroupMemberCommandSource<out T> : MemberCommandSource<T> {
    override val globalSubject: ChatGroup
    override val globalSubjectPermissionId get() = "$platform-group-${globalSubject.id}"
    override val subject get() = globalSubject
    override val subjectPermissionId get() = globalSubjectPermissionId

    override val permissionIds: List<String>
        get() = listOf(primaryPermissionId, id, globalSubjectPermissionId, platform)
}

interface MemberPrivateCommandSource<out T> : MemberCommandSource<T> {
    override val subject get() = executor
    override val subjectPermissionId get() = "$globalSubjectPermissionId-private"
}

interface ContactCommandSource<out T> : UserCommandSource<T> {
    override val globalSubject get() = null
    override val globalSubjectPermissionId get() = null
    override val subject get() = executor
    override val subjectPermissionId get() = "$platform-contact-${subject.id}"
    override val executor: Contact

    override val permissionIds: List<String>
        get() = listOf(primaryPermissionId, id, subjectPermissionId, platform)
}

object UnsupportedDeleteOpMessageReceipt : SingleMessageReceipt() {
    override val id = 0.ID

    override suspend fun delete(vararg options: DeleteOption) {
        if (StandardDeleteOption.IGNORE_ON_UNSUPPORTED !in options) {
            throw UnsupportedOperationException("Deletion is not supported for UnsupportedDeleteOpMessageReceipt.")
        }
    }
}

object NoDeleteOpMessageReceipt : SingleMessageReceipt() {
    override val id = 0.ID

    override suspend fun delete(vararg options: DeleteOption) {}
}