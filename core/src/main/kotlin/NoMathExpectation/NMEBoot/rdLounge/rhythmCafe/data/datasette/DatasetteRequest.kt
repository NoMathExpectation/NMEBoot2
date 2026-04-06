package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette

import io.ktor.resources.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class DatasetteResultShape {
    @SerialName("arrays")
    ARRAYS,

    @SerialName("objects")
    OBJECTS,

    @SerialName("array")
    ARRAY,

    @SerialName("arrayfirst")
    ARRAY_FIRST,

    @SerialName("object")
    OBJECT,
}

@Serializable
@Resource("/rdlevels.json")
data class DatasetteRequest(
    val sql: String,
    @SerialName("_shape")
    val shape: DatasetteResultShape = DatasetteResultShape.ARRAY,
) {
    companion object {
        private const val QUERY_SQL_PREFIX = "select * from rdlevels"

        fun ofPending(shape: DatasetteResultShape = DatasetteResultShape.ARRAY) = DatasetteRequest(
            "$QUERY_SQL_PREFIX where ${LevelStatus::approval.name} = 0",
            shape,
        )

        fun ofIds(vararg id: String, shape: DatasetteResultShape = DatasetteResultShape.ARRAY) =
            DatasetteRequest(
                id.joinToString(
                    prefix = "$QUERY_SQL_PREFIX where ${LevelStatus::id.name} in (",
                    postfix = ")",
                ) { "'$it'" },
                shape,
            )

        fun ofRandom(
            limit: Int = 1,
            peerReview: Boolean = true,
            shape: DatasetteResultShape = DatasetteResultShape.ARRAY
        ) = DatasetteRequest(
            "$QUERY_SQL_PREFIX ${if (peerReview) "where ${LevelStatus::approval.name} >= 10" else ""} order by random() limit $limit",
            shape,
        )
    }
}
