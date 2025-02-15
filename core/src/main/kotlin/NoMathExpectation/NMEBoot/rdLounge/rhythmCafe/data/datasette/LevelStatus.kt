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
) {
    val isPending get() = approval in 0..9
    val isApproved get() = approval in 10..Int.MAX_VALUE
    val isRejected get() = approval < 0
}

suspend fun HttpResponse.bodyToLevelStatusList() = body<List<LevelStatus>>()