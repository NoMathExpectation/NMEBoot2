package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data

import kotlinx.serialization.Serializable
import love.forte.simbot.message.OfflineURIImage.Companion.toOfflineImage
import love.forte.simbot.message.buildMessages
import java.net.URI

@Serializable
internal data class Level(
    val approval: Int,
    val artist: String,
    val artist_tokens: List<String>,
    val authors: List<String>,
    val description: String,
    val description_ct: String,
    val difficulty: Int,
    val has_classics: Boolean,
    val has_freetimes: Boolean,
    val has_freezeshots: Boolean,
    val has_holds: Boolean,
    val has_oneshots: Boolean,
    val has_skipshots: Boolean,
    val has_squareshots: Boolean,
    val has_window_dance: Boolean,
    val hue: Float,
    val icon: String,
    val id: String,
    val image: String,
    val indexed: Long? = null,
    val last_updated: Long,
    val max_bpm: Float,
    val min_bpm: Float,
    val seizure_warning: Boolean,
    val rdlevel_sha1: String,
    val single_player: Boolean,
    val song: String,
    val source: String,
    val source_iid: String,
    val tags: List<String>,
    val thumb: String,
    val two_player: Boolean,
    val url: String,
    val url2: String
) {
    fun getDifficulty() = when (difficulty) {
        0 -> "简单"
        1 -> "普通"
        2 -> "困难"
        3 -> "噩梦"
        else -> difficulty.toString()
    }

    fun peerReviewed() = when (approval) {
        in Int.MIN_VALUE..-1 -> "x"
        in 0..9 -> "-"
        in 10..Int.MAX_VALUE -> "√"
        else -> approval.toString()
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
