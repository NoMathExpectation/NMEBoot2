package NoMathExpectation.NMEBoot.command.parser.argument.ext

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.source.KookCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.OneBotCommandSource
import NoMathExpectation.NMEBoot.command.impl.source.OneBotGroupMemberCommandSource
import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.argument.ArgumentCollector
import NoMathExpectation.NMEBoot.command.parser.argument.OptionalStringArgumentCollector
import NoMathExpectation.NMEBoot.command.parser.get
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect
import NoMathExpectation.NMEBoot.message.element.Attachment
import NoMathExpectation.NMEBoot.message.element.OneBotIncomingAttachment
import NoMathExpectation.NMEBoot.message.element.findKookAttachment
import NoMathExpectation.NMEBoot.message.onebot.OneBotFileCache
import love.forte.simbot.common.id.toLong
import love.forte.simbot.common.id.toLongID

class AttachmentArgumentCollector : ArgumentCollector<AnyExecuteContext, List<Attachment>> {
    private val stringCollector = OptionalStringArgumentCollector<AnyExecuteContext>()

    override suspend fun collect(context: CommandContext<AnyExecuteContext>): List<Attachment> {
        val executor = context.source.executor
        return when (executor) {
            is OneBotCommandSource -> {
                require(executor is OneBotGroupMemberCommandSource) {
                    "请在群内使用此指令"
                }

                val keyword = stringCollector.collect(context)
                val info = OneBotFileCache[executor.subject.id.toLong(), executor.executor.id.toLong(), keyword]
                    ?: error("未找到文件")
                listOf(OneBotIncomingAttachment(executor.bot, executor.subject.id.toLongID(), info))
            }

            is KookCommandSource -> {
                context.source.originalMessage?.messages?.findKookAttachment()?.takeIf { it.isNotEmpty() }
                    ?: context.source.originalMessage?.referenceMessage?.messages?.findKookAttachment()
                    ?: listOf()
            }

            else -> error("此平台不支持获取文件")
        }
    }

    override fun buildHelp(name: String) = "[$name:file]"
}

fun InsertableCommandNode<AnyExecuteContext>.collectAttachment(name: String) =
    collect(name, AttachmentArgumentCollector())

fun CommandContext<*>.getAttachments(name: String) = get<List<Attachment>>(name)