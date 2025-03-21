package NoMathExpectation.NMEBoot.message

import NoMathExpectation.NMEBoot.util.asMessages
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.*
import love.forte.simbot.resource.toResource
import java.net.URL

private val logger = KotlinLogging.logger { }

suspend fun Image.toCICode(
    name: String = "图片",
    nsfw: Boolean = false,
): String {
    if (this !is UrlAwareImage) {
        return "[$name]"
    }

    return "[[CICode,url=${url()},name=$name,nsfw=$nsfw]]"
}

suspend fun Message.toReadableStringWithCICode(context: Actor? = null) =
    asMessages().map {
        if (it is Image) {
            return@map it.toCICode()
        }
        MessageFormatter.messageToReadableString(it, context)
    }.joinToString("")

fun String.ciCodeToImageOrElse(
    default: Message = "[未知图片]".toText(),
    nsfwDefault: Message = "[数据删除]".toText(),
): Message {
    val data = removeSurrounding("[[", "]]")
        .trim()
        .split(",")
        .filter { "=" in it }
        .map { it.split("=", limit = 2) }
        .associate { it[0].trim() to it[1].trim() }

    if (data["nsfw"]?.toBoolean() == true) {
        return nsfwDefault
    }

    val url = data["url"]?.replace('\\', '/') ?: return default

    return URL(url).toResource().toOfflineResourceImage()
}

fun String.toMessageWithCICode(): Message {
    val message = this
    return buildMessages {
        var cursor = 0
        var level = 0
        var lastCursor = 0
        while (cursor < length) {
            val char = message[cursor]
            val lastChar = message.getOrNull(cursor - 1)
            val nextChar = message.getOrNull(cursor + 1)

            if (level == 0) {
                if (char == '[' && nextChar == '[') {
                    if (cursor > lastCursor) {
                        +message.substring(lastCursor, cursor)
                    }
                    level++
                    lastCursor = cursor
                }
                cursor++
                continue
            }

            if (char == '[' && nextChar == '[') {
                level++
                cursor++
                continue
            }

            if (char == ']' && lastChar == ']') {
                if (--level <= 0) {
                    val subStr = message.substring(lastCursor, cursor + 1)
                    kotlin.runCatching {
                        +subStr.ciCodeToImageOrElse().asMessages()
                    }.onFailure {
                        logger.warn(it) { "Unable to resolve CICode: $subStr" }
                        +"[未知图片]"
                    }
                    lastCursor = cursor + 1
                }
                cursor++
                continue
            }

            cursor++
        }
        if (cursor > lastCursor) {
            +message.substring(lastCursor)
        }
    }
}