package NoMathExpectation.NMEBoot.command.impl.source.offline

import NoMathExpectation.NMEBoot.command.impl.source.CommandSource

sealed interface OfflineCommandSource {
    suspend fun toOnline(): CommandSource<*>
}

suspend fun OfflineCommandSource.toOnlineOrNull() = kotlin.runCatching {
    toOnline()
}.getOrNull()