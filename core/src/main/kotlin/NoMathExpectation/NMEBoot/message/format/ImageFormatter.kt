package NoMathExpectation.NMEBoot.message.format

import NoMathExpectation.NMEBoot.message.ResourceCache
import NoMathExpectation.NMEBoot.util.defaultHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.call.*
import io.ktor.client.request.*
import love.forte.simbot.common.id.StringID.Companion.ID
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.*
import love.forte.simbot.resource.toResource
import org.koin.core.annotation.Single

@Single
class ImageFormatter : MessageElementFormatter<Image> {
    override val type = "image"
    override val formatClass = Image::class

    private val logger = KotlinLogging.logger { }

    override suspend fun toReadableString(element: Image, context: Actor?): String {
        return "[图片]"
    }

    override suspend fun serialize(element: Image, context: Actor?): List<String> {
        if (element is RemoteImage) {
            val id = element.id
            return listOf(type, "id", id.toString())
        }

        if (element is UrlAwareImage) {
            kotlin.runCatching {
                val resource = defaultHttpClient.get(element.url())
                    .body<ByteArray>()
                    .toResource()
                val item = ResourceCache.put(resource, ResourceCache.Item.Type.IMAGE)
                val id = item.id
                return listOf(type, "cache", id.toString())
            }
        }
        logger.warn { "Unknown image: $element" }
        return listOf(type, "unknown")
    }

    override suspend fun deserialize(segments: List<String>, context: Actor?): Image {
        return when (segments[1]) {
            "id" -> RemoteIDImage(segments[2].ID)
            "cache" -> {
                val resource = ResourceCache.get(segments[2].ID)
                    ?.toResource()
                    ?: this::class.java
                        .getResource("/unknown.png")!!
                        .toResource()
                resource.toOfflineResourceImage()
            }

            else -> {
                logger.warn { "Unknown image format: $segments" }
                this::class.java
                    .getResource("/unknown.png")!!
                    .toResource()
                    .toOfflineResourceImage()
            }
        }
    }
}