package NoMathExpectation.NMEBoot.command.impl.source.offline

import NoMathExpectation.NMEBoot.command.impl.source.OneBotFriendCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.OneBotGroupMemberCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.OneBotGroupMemberPrivateCommandSource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.ID
import love.forte.simbot.component.onebot.v11.core.actor.OneBotFriend
import love.forte.simbot.component.onebot.v11.core.actor.OneBotMember

@Serializable
@SerialName("onebot.group.member")
data class OfflineOneBotGroupMemberCommandSource(
    val botId: ID,
    val groupId: ID,
    val memberId: ID,
) : OfflineCommandSource<OneBotMember> {
    override suspend fun toOnline() = OneBotGroupMemberCommandSource.Data(botId, groupId, memberId)
}

@Serializable
@SerialName("onebot.group.member.private")
data class OfflineOneBotGroupMemberPrivateCommandSource(
    val botId: ID,
    val groupId: ID,
    val memberId: ID,
) : OfflineCommandSource<OneBotMember> {
    override suspend fun toOnline() = OneBotGroupMemberPrivateCommandSource.Data(botId, groupId, memberId)
}

@Serializable
@SerialName("onebot.friend")
data class OfflineOneBotFriendCommandSource(
    val botId: ID,
    val friendId: ID,
) : OfflineCommandSource<OneBotFriend> {
    override suspend fun toOnline() = OneBotFriendCommandSource.Data(botId, friendId)
}