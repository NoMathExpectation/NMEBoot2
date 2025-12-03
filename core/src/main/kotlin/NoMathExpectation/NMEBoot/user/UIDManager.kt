package NoMathExpectation.NMEBoot.user

import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.impl.source.ConsoleCommandSource
import NoMathExpectation.NMEBoot.util.storageOf
import kotlinx.serialization.Serializable

object UIDManager {
    @Serializable
    data class Data(
        var nextAllocate: Long = 1,
        val mappings: MutableMap<String, Long> = mutableMapOf(
            ConsoleCommandSource.id to ConsoleCommandSource.uid,
        ),
    )

    private val data = storageOf("data/uid.json", Data())

    suspend fun fromId(id: String): Long {
        return data.referenceUpdate {
            it.mappings.getOrPut(id) { it.nextAllocate++ }
        }
    }

    suspend fun getAll() = data.get().mappings.toMap()
}

internal suspend fun CommandSource<*>.idToUid() = UIDManager.fromId(id)