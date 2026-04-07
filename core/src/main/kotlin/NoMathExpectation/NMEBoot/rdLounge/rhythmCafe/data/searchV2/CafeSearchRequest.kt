package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.searchV2

import io.ktor.resources.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api/levels")
internal data class CafeSearchRequest(
    val q: String = "",
    @SerialName("per_page")
    val perPage: Int = 5,
    val page: Int = 1,
    @SerialName("peer_review")
    val peerReview: PeerReviewFilter = PeerReviewFilter.ALL,
) {
    @Serializable
    enum class PeerReviewFilter {
        @SerialName("approved")
        APPROVED,

        @SerialName("pending")
        PENDING,

        @SerialName("rejected")
        REJECTED,

        @SerialName("all")
        ALL
    }

    companion object {
        const val MAX_PER_PAGE = 100
    }
}
