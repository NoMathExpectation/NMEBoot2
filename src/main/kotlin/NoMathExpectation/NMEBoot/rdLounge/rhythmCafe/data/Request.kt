package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data

import io.ktor.resources.*
import kotlinx.serialization.Serializable

@Serializable
@Resource("/typesense/collections/levels/documents/search")
internal data class Request(
    val q: String = "",
    val query_by: String = "song, authors, artist, tags, description",
    val query_by_weights: String = "12, 8, 6, 5, 4",
    val facet_by: String = "authors,tags,source,difficulty,artist",
    val per_page: Int = 5,
    val max_facet_values: Int = 10,
    val filter_by: String = "approval:=[-1..20]",
    val page: Int = 1,
    val sort_by: String = "_text_match:desc,indexed:desc,last_updated:desc",
    val num_typos: String = "2, 1, 1, 1, 0"
) {
    companion object {
        const val PR = "approval:=[10..20]"
        const val ANY = "approval:=[-1..20]"
        const val PENDING = "approval:=[0..9]"

        const val MAX_PER_PAGE = 250
    }
}
