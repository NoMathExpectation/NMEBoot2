package NoMathExpectation.NMEBoot.command.impl.source.offline

import NoMathExpectation.NMEBoot.command.impl.source.CommandSource

sealed interface OfflineCommandSource<out T> {
    suspend fun toOnline(): CommandSource<T>
}

typealias AnyOfflineCommandSource = OfflineCommandSource<*>

suspend fun <T> OfflineCommandSource<T>.toOnlineOrNull() = kotlin.runCatching {
    toOnline()
}.getOrNull()