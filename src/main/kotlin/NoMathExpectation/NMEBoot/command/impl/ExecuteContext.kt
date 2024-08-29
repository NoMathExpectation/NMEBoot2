package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import love.forte.simbot.ability.ReplySupport
import love.forte.simbot.ability.SendSupport
import love.forte.simbot.message.MessageContent

data class ExecuteContext<E, T, R>(
    val executor: CommandSource<E>,
    val target: CommandSource<T>,
    val recipient: CommandSource<R>,
    val originalMessage: MessageContent? = null,
) : PermissionAware, SendSupport by recipient, ReplySupport by recipient {
    override suspend fun hasPermission(permission: String) = executor.hasPermission(permission)

    override suspend fun setPermission(permission: String, value: Boolean?) = target.setPermission(permission, value)

    class Builder<E, T, R>(
        val executor: CommandSource<E>,
        val target: CommandSource<T>,
        val recipient: CommandSource<R>,
    ) {
        var originalMessage: MessageContent? = null

        fun build(): ExecuteContext<E, T, R> = ExecuteContext(
            executor,
            target,
            recipient,
            originalMessage,
        )
    }
}

inline fun <E, T, R> ExecuteContext(
    executor: CommandSource<E>,
    target: CommandSource<T>,
    recipient: CommandSource<R>,
    buildBlock: ExecuteContext.Builder<E, T, R>.() -> Unit
): ExecuteContext<E, T, R> = ExecuteContext.Builder(executor, target, recipient).apply(buildBlock).build()

inline fun <S> ExecuteContext(from: CommandSource<S>, buildBlock: ExecuteContext.Builder<S, S, S>.() -> Unit) =
    ExecuteContext(from, from, from, buildBlock)