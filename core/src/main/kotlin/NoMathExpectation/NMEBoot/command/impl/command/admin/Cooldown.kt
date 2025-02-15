package NoMathExpectation.NMEBoot.command.impl.command.admin

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.source.requiresBotModerator
import NoMathExpectation.NMEBoot.command.parser.argument.collectLong
import NoMathExpectation.NMEBoot.command.parser.argument.getLong
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.util.FixedDelayUseCounter
import NoMathExpectation.NMEBoot.util.UseCounter
import NoMathExpectation.NMEBoot.util.storageOf
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.seconds

@Serializable
data class CooldownConfig(
    val defaultCooldown: Long = 5L,
    val privateTimers: MutableMap<Long, UseCounter> = mutableMapOf(),
    val groupConfigs: MutableMap<String, GroupConfig> = mutableMapOf(),
) {
    @Serializable
    data class GroupConfig(
        var cooldown: Long = 10,
        val timers: MutableMap<Long, UseCounter> = mutableMapOf(),
    )

    fun getGroup(id: String) = groupConfigs.getOrPut(id) { GroupConfig() }

    fun getGroupUser(groupId: String, userId: Long) =
        groupConfigs.getOrPut(groupId) { GroupConfig() }.let {
            it.timers.getOrPut(userId) { FixedDelayUseCounter(1, it.cooldown.seconds) }
        }

    fun getPrivateUser(userId: Long) =
        privateTimers.getOrPut(userId) { FixedDelayUseCounter(1, defaultCooldown.seconds) }

    companion object {
        val storage = storageOf<CooldownConfig>("config/cooldown.json", CooldownConfig())
    }
}

fun LiteralSelectionCommandNode<AnyExecuteContext>.commandCooldown() =
    literal("cooldown", "cd")
        .requiresBotModerator()
        .collectLong("seconds")
        .executes("设置指令冷却") {
            val seconds = getLong("seconds") ?: error("Argument \"seconds\" is required.")
            val globalSubjectPermissionId = it.target.globalSubjectPermissionId ?: run {
                it.reply("此平台不支持调整冷却")
                return@executes
            }

            CooldownConfig.storage.referenceUpdate {
                val groupConfig = it.getGroup(globalSubjectPermissionId)
                groupConfig.cooldown = seconds
                groupConfig.timers.clear()
            }

            it.reply("已设置冷却为${seconds}秒")
        }