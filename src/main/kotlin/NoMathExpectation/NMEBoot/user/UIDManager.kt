package NoMathExpectation.NMEBoot.user

import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.command.source.ConsoleCommandSource
import NoMathExpectation.NMEBoot.util.storageOf
import kotlinx.serialization.Serializable
import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.IntID.Companion.ID
import love.forte.simbot.common.id.LongID.Companion.ID

object UIDManager {
    @Serializable
    data class Data(
        var nextAllocate: Long = 1,
        val mappings: MutableMap<String, ID> = mutableMapOf(
            ConsoleCommandSource.id to ConsoleCommandSource.uid,
        ),
    )

    private val data = storageOf("data/uid.json", Data())

    suspend fun fromId(id: String): ID {
        var result: ID = (-1).ID
        data.referenceUpdate {
            result = it.mappings.getOrPut(id) { (it.nextAllocate++).ID }
        }
        return result
    }
}

internal suspend fun CommandSource<*>.idToUid() = UIDManager.fromId(id)