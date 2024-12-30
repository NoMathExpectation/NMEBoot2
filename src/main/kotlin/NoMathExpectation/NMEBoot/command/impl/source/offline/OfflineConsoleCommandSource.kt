package NoMathExpectation.NMEBoot.command.impl.source.offline

import NoMathExpectation.NMEBoot.command.impl.source.ConsoleCommandSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("console")
data object OfflineConsoleCommandSource : OfflineCommandSource<Nothing?> {
    override suspend fun toOnline() = ConsoleCommandSource
}