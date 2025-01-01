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
@Resource("/datasette/combined.json")
data class DatasetteRequest(
    val sql: String,
    @SerialName("_shape")
    val shape: DatasetteResultShape = DatasetteResultShape.ARRAY,
) {
    companion object {
        private const val QUERY_SQL_PREFIX = "select id, artist, song, authors, approval from combined"

        fun ofPending(shape: DatasetteResultShape = DatasetteResultShape.ARRAY) = DatasetteRequest(
            "$QUERY_SQL_PREFIX where approval = 0",
            shape,
        )

        fun ofIds(vararg id: String, shape: DatasetteResultShape = DatasetteResultShape.ARRAY) =
            DatasetteRequest(
                id.joinToString(
                    prefix = "$QUERY_SQL_PREFIX where id in (",
                    postfix = ")",
                ) { "'$it'" },
                shape,
            )
    }
}
