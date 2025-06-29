package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.message.toSerialized
import NoMathExpectation.NMEBoot.message.unescapeMessageFormatIdentifiers
import NoMathExpectation.NMEBoot.scripting.data.EvalRequest
import NoMathExpectation.NMEBoot.scripting.data.EvalResponse
import NoMathExpectation.NMEBoot.util.defaultHttpClient
import NoMathExpectation.NMEBoot.util.storageOf
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
private data class EvalConfig(
    val endpoint: String = "http://scripting:8080/eval",
    val timeout: Long = 30 * 1000
)

private val configStorage = storageOf("config/eval.json", EvalConfig())

private val logger = KotlinLogging.logger { }

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandEval() =
    literal("eval")
        .requiresPermission("command.common.eval")
        .collectGreedyString("code")
        .executes("运行Kotlin代码") {
            val code = getString("code")?.unescapeMessageFormatIdentifiers() ?: error("Code required.")
            val quoted = it.originalMessage?.referenceMessage()?.messages?.toSerialized(it.target.globalSubject)

            val (endpoint, timeout) = configStorage.get()

            val result = kotlin.runCatching {
                defaultHttpClient.post(endpoint) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        EvalRequest(
                            code,
                            null,
                            quoted,
                            timeout,
                        )
                    )
                    timeout {
                        requestTimeoutMillis = timeout + 10 * 1000
                        connectTimeoutMillis = null
                        socketTimeoutMillis = null
                    }
                }.body<EvalResponse>()
            }.getOrElse { exception ->
                logger.error(exception) { "请求脚本服务器失败" }
                it.reply("请求失败")
                return@executes
            }

            it.reply(
                listOfNotNull(result.output, result.returns, result.exception)
                    .filter(String::isNotBlank)
                    .joinToString("\n")
                    .ifEmpty { "无输出" }
            )
        }