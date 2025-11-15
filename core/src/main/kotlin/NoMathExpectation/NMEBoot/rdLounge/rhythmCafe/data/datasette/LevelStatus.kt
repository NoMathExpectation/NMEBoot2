package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette

import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import love.forte.simbot.kook.util.BooleanToIntSerializer
import love.forte.simbot.message.OfflineURIImage.Companion.toOfflineImage
import love.forte.simbot.message.buildMessages
import java.net.URI

private class StringRepresentationListSerializer : KSerializer<List<String>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringRepresentationToListSerializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: List<String>) {
        encoder.encodeString(value.joinToString(",", "[", "]") { "\"$it\"" })
    }

    override fun deserialize(decoder: Decoder): List<String> {
        return decoder.decodeString()
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.removeSurrounding("\"") }
    }
}

@Serializable
data class LevelStatus(
    val id: String,
    val artist: String,
    val song: String,
    val authors: @Serializable(StringRepresentationListSerializer::class) List<String>,
    val approval: Int,
    val image: String,
    val difficulty: Int,
    val seizure_warning: @Serializable(BooleanToIntSerializer::class) Boolean,
    val description: String,
    val single_player: @Serializable(BooleanToIntSerializer::class) Boolean,
    val two_player: @Serializable(BooleanToIntSerializer::class) Boolean,
    val tags: @Serializable(StringRepresentationListSerializer::class) List<String>,
    val url: String,
    val url2: String,
) {
    val isPending get() = approval in 0..9
    val isApproved get() = approval in 10..Int.MAX_VALUE
    val isRejected get() = approval < 0

    fun peerReviewed() = when (approval) {
        in Int.MIN_VALUE..-1 -> "x"
        in 0..9 -> "-"
        in 10..Int.MAX_VALUE -> "√"
        else -> approval.toString()
    }

    fun getDifficulty() = when (difficulty) {
        0 -> "简单"
        1 -> "普通"
        2 -> "困难"
        3 -> "噩梦"
        else -> difficulty.toString()
    }

    fun toDetailedMessage() = buildMessages {
        +URI(image).toOfflineImage()

        +"歌曲名: $song\n"

        +"作曲家: $artist\n"

        +"作者: ${authors.joinToString()}\n"

        +"难度: ${getDifficulty()}\n"

        if (seizure_warning) {
            +"癫痫警告!\n"
        }

        +"同行评审: ${peerReviewed()}\n"

        +"描述:\n$description\n"

        +"模式: "
        if (single_player) {
            +"1p "
        }
        if (two_player) {
            +"2p "
        }
        +"\n"

        +"标签: ${tags.joinToString()}\n"

        +url2
    }
}

suspend fun HttpResponse.bodyToLevelStatusList() = body<List<LevelStatus>>()