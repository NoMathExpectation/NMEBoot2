package NoMathExpectation.NMEBoot.message.onebot.apiExt.lagrange

import NoMathExpectation.NMEBoot.message.element.Attachment
import NoMathExpectation.NMEBoot.message.onebot.CopiedOneBotMessageReceipt
import NoMathExpectation.NMEBoot.message.onebot.apiExt.OnebotExtApi
import NoMathExpectation.NMEBoot.message.onebot.apiExt.dataOrThrowCustom
import NoMathExpectation.NMEBoot.message.onebot.apiExt.toOneBotGroupUploadApi
import NoMathExpectation.NMEBoot.message.onebot.apiExt.toOneBotPrivateUploadApi
import love.forte.simbot.common.id.LongID
import love.forte.simbot.common.id.StringID
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.event.notice.RawGroupUploadEvent
import love.forte.simbot.component.onebot.v11.message.segment.OneBotForwardNode
import love.forte.simbot.message.MessageReceipt

class OnebotLagrangeExtApi(private val bot: OneBotBot) : OnebotExtApi {
    override val type = OnebotExtApi.Type.LAGRANGE

    override suspend fun deleteGroupFile(
        groupId: LongID,
        fileInfo: RawGroupUploadEvent.FileInfo
    ) {
        val api = LagrangeDeleteGroupFile.create(groupId, fileInfo)
        bot.executeResult(api).dataOrThrowCustom { "删除群文件失败：${fileInfo.name}" }
    }

    override suspend fun getGroupFileUrl(
        groupId: LongID,
        fileInfo: RawGroupUploadEvent.FileInfo
    ): String {
        val api = LagrangeGetGroupFileUrl.create(groupId, fileInfo)
        val result = bot.executeResult(api)
        return result.dataOrThrowCustom { "获取文件链接失败：${fileInfo.name}" }.url
    }

    override suspend fun sendGroupForwardMsg(
        groupId: LongID,
        messages: List<OneBotForwardNode>
    ): MessageReceipt {
        val api = LagrangeSendGroupForwardMsg.create(groupId, messages)
        val result = bot.executeResult(api)
        return CopiedOneBotMessageReceipt(
            result.dataOrThrowCustom { "发送转发消息失败" }.messageId,
            bot,
        )
    }

    override suspend fun sendPrivateForwardMsg(
        userId: LongID,
        messages: List<OneBotForwardNode>
    ): MessageReceipt {
        val api = LagrangeSendPrivateForwardMsg.create(userId, messages)
        val result = bot.executeResult(api)
        return CopiedOneBotMessageReceipt(
            result.dataOrThrowCustom { "发送转发消息失败" }.messageId,
            bot,
        )
    }

    override suspend fun uploadGroupFile(
        attachment: Attachment,
        groupId: LongID,
        folderId: StringID?
    ) {
        val api = attachment.toOneBotGroupUploadApi(groupId)
        bot.executeResult(api).dataOrThrowCustom { "上传文件失败：${attachment.name}" }
    }

    override suspend fun uploadPrivateFile(
        attachment: Attachment,
        userId: LongID
    ) {
        val api = attachment.toOneBotPrivateUploadApi(userId)
        bot.executeResult(api).dataOrThrowCustom { "上传文件失败：${attachment.name}" }
    }
}