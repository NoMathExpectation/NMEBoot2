package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.message.FormatOptions
import NoMathExpectation.NMEBoot.message.ResourceCache
import NoMathExpectation.NMEBoot.util.defaultHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.*
import io.ktor.client.request.*
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.*
import love.forte.simbot.message.OfflineImage.Companion.toOfflineImage
import love.forte.simbot.resource.toResource
import org.koin.core.annotation.Single
import java.net.URL
import kotlin.io.encoding.Base64

@Single
class ImageFormatter : MessageElementFormatter<Image> {
    override val type = "image"
    override val formatClass = Image::class

    private val logger = KotlinLogging.logger { }

    override suspend fun toReadableString(element: Image, context: Actor?, options: FormatOptions): String {
        return "[图片]"
    }

    private suspend fun persistentSerialize(element: Image, context: Actor?, options: FormatOptions): List<String> {
        if (element is UrlAwareImage) {
            val url = element.url()
            kotlin.runCatching {
                val data = defaultHttpClient.get(url)
                    .body<ByteArray>()

                val base64 = Base64.encode(data)

                return listOf(type, "base64", base64)
            }.getOrElse {
                logger.error { "Failed to persist for image: $url" }
            }
        }

        if (element is OfflineImage) {
            val data = element.data()
            val base64 = Base64.encode(data)
            return listOf(type, "base64", base64)
        }

        if (element is RemoteImage) {
            val id = element.id
            return listOf(type, "id", id.toString())
        }

        logger.warn { "Unknown image: $element" }
        return listOf(type, "unknown")
    }

    override suspend fun serialize(element: Image, context: Actor?, options: FormatOptions): List<String> {
        if (options.persistent) {
            return persistentSerialize(element, context, options)
        }

        if (element is RemoteImage) {
            val id = element.id
            return listOf(type, "id", id.toString())
        }

        if (element is UrlAwareImage) {
            val url = element.url()
            kotlin.runCatching {
                val resource = defaultHttpClient.get(url)
                    .body<ByteArray>()
                    .toResource()

                val item = ResourceCache.put(resource, ResourceCache.Item.Type.IMAGE)
                val id = item.id
                return listOf(type, "cache", id.toString())
            }.getOrElse {
                logger.error { "Failed to store cache for image: $url" }
            }
        }

        if (element is OfflineImage) {
            val resource = element.data().toResource()
            val item = ResourceCache.put(resource, ResourceCache.Item.Type.IMAGE)
            val id = item.id
            return listOf(type, "cache", id.toString())
        }

        logger.warn { "Unknown image: $element" }
        return listOf(type, "unknown")
    }

    override suspend fun deserialize(segments: List<String>, context: Actor?, options: FormatOptions): Image {
        return when (segments[1]) {
            "id" -> RemoteIDImage(segments[2].ID)
            "cache" -> {
                ResourceCache.get(segments[2].ID)
                    ?.toResource()
                    ?.toOfflineResourceImage()
                    ?: unknownImage
            }
            "url" -> {
                val url = segments[2]
                URL(url).toResource().toOfflineResourceImage()
            }
            "base64" -> {
                val base64 = segments[2]
                val data = Base64.decode(base64)
                data.toOfflineImage()
            }

            "unknown" -> unknownImage

            else -> {
                logger.warn { "Unknown image format: $segments" }
                unknownImage
            }
        }
    }

    companion object {
        val unknownImage by lazy {
            this::class.java
                .getResource("/unknown.png")!!
                .readBytes()
                .toOfflineImage()
        }
    }
}