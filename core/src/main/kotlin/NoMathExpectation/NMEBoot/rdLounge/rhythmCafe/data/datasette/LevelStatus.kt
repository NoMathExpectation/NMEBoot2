package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette

import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
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
            .map { it.trim().removeSurrounding("\"") }
    }
}

@Serializable
data class LevelStatus(
    val id: String,
    val artist: String,
    @SerialName("artist_tokens")
    val artistTokens: @Serializable(StringRepresentationListSerializer::class) List<String>,
    val song: String,
    val authors: @Serializable(StringRepresentationListSerializer::class) List<String>,
    @SerialName("authors_raw")
    val rawAuthors: String,
    val approval: Int,
    @SerialName("image_url")
    val imageUrl: String,
    val difficulty: Int,
    @SerialName("seizure_warning")
    val seizureWarning: @Serializable(BooleanToIntSerializer::class) Boolean,
    val description: String,
    @SerialName("single_player")
    val singlePlayer: @Serializable(BooleanToIntSerializer::class) Boolean,
    @SerialName("two_player")
    val twoPlayer: @Serializable(BooleanToIntSerializer::class) Boolean,
    val tags: @Serializable(StringRepresentationListSerializer::class) List<String>,
    @SerialName("rdzip_url")
    val rdzipUrl: String,
) {
    val isPending get() = approval in 0..9
    val isApproved get() = approval in 10..Int.MAX_VALUE
    val isRejected get() = approval < 0

    val peerReviewStatus
        get() = when (approval) {
        in Int.MIN_VALUE..-1 -> "x"
        in 0..9 -> "-"
        in 10..Int.MAX_VALUE -> "√"
        else -> approval.toString()
    }

    val difficultyText
        get() = when (difficulty) {
        0 -> "简单"
        1 -> "普通"
        2 -> "困难"
        3 -> "噩梦"
        else -> difficulty.toString()
    }

    fun toDetailedMessage() = buildMessages {
        +URI(imageUrl).toOfflineImage()

        +"歌曲名: $song\n"

        +"作曲家: $artist\n"

        +"作者: $rawAuthors\n"

        +"难度: $difficultyText\n"

        if (seizureWarning) {
            +"癫痫警告!\n"
        }

        +"同行评审: $peerReviewStatus\n"

        +"描述:\n$description\n"

        +"模式: "
        if (singlePlayer) {
            +"1p "
        }
        if (twoPlayer) {
            +"2p "
        }
        +"\n"

        +"标签: ${tags.joinToString()}\n"

        +rdzipUrl
    }
}

suspend fun HttpResponse.bodyToLevelStatusList() = body<List<LevelStatus>>()