package NoMathExpectation.NMEBoot.command.impl.source.offline

import NoMathExpectation.NMEBoot.command.impl.source.KookChannelCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.KookPrivateCommandSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.ID

@Serializable
@SerialName("kook.channel")
data class OfflineKookChannelCommandSource(
    val botId: ID,
    val guildId: ID,
    val channelId: ID,
    val memberId: ID,
) : OfflineCommandSource {
    override suspend fun toOnline() = KookChannelCommandSource.Data(botId, guildId, channelId, memberId)
}

@Serializable
@SerialName("kook.private")
data class OfflineKookPrivateCommandSource(
    val botId: ID,
    val userId: ID,
) : OfflineCommandSource {
    override suspend fun toOnline() = KookPrivateCommandSource.Data(botId, userId)
}