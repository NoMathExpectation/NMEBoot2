package NoMathExpectation.NMEBoot.message.format.onebot

import NoMathExpectation.NMEBoot.message.FormatOptions
import NoMathExpectation.NMEBoot.message.MessageFormatter
import NoMathExpectation.NMEBoot.message.format.ImageFormatter
import NoMathExpectation.NMEBoot.message.format.MessageElementFormatter
import NoMathExpectation.NMEBoot.message.format.PlainTextFormatter
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.annotations.ExperimentalSimbotAPI
import love.forte.simbot.common.id.IntID.Companion.ID
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.component.onebot.common.annotations.ExperimentalOneBotAPI
import love.forte.simbot.component.onebot.common.annotations.InternalOneBotAPI
import love.forte.simbot.component.onebot.v11.core.bot.OneBotBot
import love.forte.simbot.component.onebot.v11.message.resolveToMessageElement
import love.forte.simbot.component.onebot.v11.message.segment.*
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.toMessages
import org.koin.core.annotation.Single

@Single
class OneBotMessageSegmentElementFormatter(
    private val diceFormatter: OneBotDiceFormatter,
    private val imageFormatter: ImageFormatter,
    private val textFormatter: PlainTextFormatter,
) : MessageElementFormatter<OneBotMessageSegmentElement> {
    override val type = "obs"
    override val formatClass = OneBotMessageSegmentElement::class

    private val logger = KotlinLogging.logger { }

    override suspend fun toReadableString(
        element: OneBotMessageSegmentElement,
        context: Actor?,
        options: FormatOptions
    ): String {
        return "[onebot消息段]"
    }

    @OptIn(InternalOneBotAPI::class, ExperimentalOneBotAPI::class, ExperimentalSimbotAPI::class)
    suspend fun serializeForwardNode(
        segment: OneBotForwardNode,
        context: Actor?,
        options: FormatOptions
    ): List<String> {
        val data = segment.data
        // Not persisting forward for now since deserializing custom forward requires sending message.
//        if (options.persistent && data.id != null) {
//            val bot = options.bot
//            when (bot) {
//                null -> {
//                    logger.warn(UnsupportedOperationException()) { "Trying to persist forward node message but bot is null." }
//                }
//                !is OneBotBot -> {
//                    logger.warn { "Trying to persist forward message but bot is not OnebotBot!" }
//                }
//
//                else -> {
//                    val result = bot.executeData(GetMsgApi.create(data.id!!))
//                    val sender = result.sender
//                    data = OneBotForwardNode.Data(
//                        null,
//                        (sender["user_id"] as? JsonPrimitive)?.content?.ID ?: (-1).ID,
//                        sender["nickname"]?.toString() ?: "QQ用户",
//                        result.message
//                    )
//                }
//            }
//        }

        if (data.id != null) {
            return listOf(type, "node", "id", data.id.toString())
        }

        return listOf(
            type,
            "node",
            "data",
            data.userId?.toString() ?: "-1",
            data.nickname ?: "QQ用户",
            data.content?.map { it.resolveToMessageElement() }?.toMessages()
                ?.let { MessageFormatter.messageToSerialized(it, context, options) } ?: "",
        )
    }

    @OptIn(ExperimentalOneBotAPI::class, InternalOneBotAPI::class)
    suspend fun deserializeForwardNode(
        segments: List<String>,
        context: Actor?,
        options: FormatOptions
    ): OneBotForwardNode {
        val idIndex = segments.indexOf("id")
        if (idIndex != -1 && idIndex + 1 < segments.size) {
            val id = segments[idIndex + 1]
            return OneBotForwardNode.create(id.ID)
        }

        val dataIndex = segments.indexOf("data")
        if (dataIndex != -1 && dataIndex + 3 < segments.size) {
            val userId = segments[dataIndex + 1].ID
            val nickname = segments[dataIndex + 2]
            val contentSerialized = segments[dataIndex + 3]
            val content = MessageFormatter.deserializeMessage(contentSerialized, context, options)
            return OneBotForwardNode.create(
                userId = userId,
                nickname = nickname,
                content = content,
                (options.bot as? OneBotBot)?.configuration?.defaultImageAdditionalParamsProvider
            )
        }

        logger.warn { "Invalid forward node segment: $segments" }
        return OneBotForwardNode.create(
            userId = (-1).ID,
            nickname = "QQ用户",
            content = emptyList()
        )
    }

    @OptIn(ExperimentalOneBotAPI::class)
    fun serializeForward(segment: OneBotForward, context: Actor?, options: FormatOptions): List<String> {
        // Not persisting forward for now since deserializing custom forward requires sending message.
        return listOf(type, "forward", "id", segment.id.toString())

//        if (!options.persistent) {
//            return listOf(type, "forward", "id", segment.id.toString())
//        }
//
//        val bot = options.bot
//        if (bot == null) {
//            logger.warn(UnsupportedOperationException()) { "Trying to persist forward message but bot is null." }
//            return listOf(type, "forward", "id", segment.id.toString())
//        }
//        if (bot !is OneBotBot) {
//            logger.warn { "Trying to persist forward message but bot is not OnebotBot!" }
//            return listOf(type, "forward", "id", segment.id.toString())
//        }
//
//        return listOf(type, "forward", "list") + segment.getForwardNodes(bot).map {
//            MessageFormatter.messageElementToSerialized(it.toElement(), context, options)
//        }
    }

    fun deserializeForward(segments: List<String>, context: Actor?, options: FormatOptions): OneBotForward {
        val idIndex = segments.indexOf("id")
        if (idIndex != -1 && idIndex + 1 < segments.size) {
            val id = segments[idIndex + 1]
            return OneBotForward.create(id.ID)
        }

        // Not supporting custom forward deserialization for now since it requires sending message.
//        val listIndex = segments.indexOf("list")
//        if (listIndex != -1 && listIndex + 1 < segments.size) {
//            val nodesSerialized = segments.subList(listIndex + 1, segments.size)
//            val nodes = nodesSerialized.map { (MessageFormatter.deserializeMessageElement(it, context, options) as DefaultOneBotMessageSegmentElement).oneBotSegment<OneBotForwardNode>() }
//            return OneBotForward.create(nodes)
//        }

        logger.warn { "Invalid forward segment: $segments" }
        return OneBotForward.create((-1).ID)
    }

    override suspend fun serialize(
        element: OneBotMessageSegmentElement,
        context: Actor?,
        options: FormatOptions
    ): List<String> {
        when (element) {
            is OneBotDice.Element -> return diceFormatter.serialize(element, context, options)
            is OneBotText.Element -> return textFormatter.serialize(element, context, options)
            is OneBotImage.Element -> return imageFormatter.serialize(element, context, options)
        }

        return when (val obSegment = element.segment) {
            is OneBotJson -> listOf(type, "json", obSegment.data.data)
            is OneBotXml -> listOf(type, "xml", obSegment.data.data)
            is OneBotReply -> listOf("ref", obSegment.id.toString())
            is OneBotForward -> serializeForward(obSegment, context, options)
            is OneBotForwardNode -> serializeForwardNode(obSegment, context, options)
            else -> {
                logger.warn { "Unknown onebot segment: $obSegment" }
                listOf(type, "unknown")
            }
        }
    }

    override suspend fun deserialize(
        segments: List<String>,
        context: Actor?,
        options: FormatOptions
    ): OneBotMessageSegmentElement {
        return when (segments.getOrNull(1)) {
            "json" -> OneBotJson.create(segments[2])
            "xml" -> OneBotXml.create(segments[2])
            "dice" -> OneBotDice
            "text" -> OneBotText.create(segments.getOrNull(2) ?: "")
            "reply" -> OneBotReply.create(segments[2].ID)
            "forward" -> deserializeForward(segments, context, options)
            "node" -> deserializeForwardNode(segments, context, options)
            "unknown" -> OneBotText.create("")
            else -> {
                logger.warn { "Unknown onebot segment: $segments" }
                OneBotText.create("")
            }
        }.toElement()
    }
}