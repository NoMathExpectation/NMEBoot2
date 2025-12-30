package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.isDebug
import NoMathExpectation.NMEBoot.command.impl.requiresGlobalSubjectId
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.database.message.MessageHistory
import NoMathExpectation.NMEBoot.message.onebot.OneBotFolding
import NoMathExpectation.NMEBoot.message.stringifyUserMentions
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.component.onebot.v11.core.event.notice.OneBotPokeEvent
import love.forte.simbot.message.plus
import kotlin.time.measureTime

private const val HISTORY_PERMISSION_NAME = "command.common.history"

private val logger = KotlinLogging.logger { }

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandHistory() =
    literal("history")
        .requiresGlobalSubjectId()
        .requiresPermission(HISTORY_PERMISSION_NAME)
        .executes("随机一条历史消息") {
            val globalSubject = it.target.globalSubject ?: error("请在组织内使用此指令。")
            val platform = it.target.platform

            MessageHistory.fetchRandomMessage(platform, globalSubject.id.toString(), globalSubject)?.let { pair ->
                val (senderName, message) = pair
                it.reply("$senderName 曾经说过：")
                it.send(OneBotFolding.FoldIgnore + message.stringifyUserMentions(globalSubject))
                return@executes
            }

            it.reply("好像没有什么黑历史...")
        }

suspend fun pokeEventForHistory(event: OneBotPokeEvent) {
    val context = CommandSource.get(event) ?: return
    if (!context.hasPermission(HISTORY_PERMISSION_NAME)) {
        return
    }

    val globalSubject = context.globalSubject ?: return
    val platform = context.platform

    val time = measureTime {
        runCatching {
            MessageHistory.fetchRandomMessage(platform, globalSubject.id.toString(), globalSubject)?.let { pair ->
                val (_, message) = pair
                context.send(OneBotFolding.FoldIgnore + message.stringifyUserMentions(globalSubject))
            }
        }.onFailure {
            logger.error(it) { "Error while poking for history:" }
            if (context.isDebug()) {
                context.send(it.message ?: "未知错误")
            }
        }
    }
    logger.info { "Process time: $time" }
}