package NoMathExpectation.NMEBoot.message.onebot

import love.forte.simbot.ability.DeleteOption
import love.forte.simbot.ability.StandardDeleteOption
import love.forte.simbot.common.id.ID
import love.forte.simbot.component.onebot.v11.core.api.DeleteMsgApi
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.message.OneBotMessageReceipt
import love.forte.simbot.message.SingleMessageReceipt

// copied from sdk
class CopiedOneBotMessageReceipt(
    override val messageId: ID,
    private val bot: OneBotBot,
) : SingleMessageReceipt(), OneBotMessageReceipt {
    override val id = messageId

    override suspend fun delete(vararg options: DeleteOption) {
        kotlin.runCatching {
            bot.executeData(DeleteMsgApi.create(messageId))
        }.onFailure { ex ->
            if (StandardDeleteOption.IGNORE_ON_FAILURE !in options) {
                throw ex
            }
        }
    }
}