package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.command.source.CommandSource
import NoMathExpectation.NMEBoot.command.util.requiresPermission
import NoMathExpectation.NMEBoot.util.FixedRateUseCounter
import NoMathExpectation.NMEBoot.util.UseCounter
import NoMathExpectation.NMEBoot.util.mutableMapStorageOf
import NoMathExpectation.NMEBoot.util.referenceUpdate
import kotlinx.serialization.Serializable
import love.forte.simbot.message.buildMessages
import kotlin.random.Random

@Serializable
class Luck(var counter: UseCounter = FixedRateUseCounter.ofDay(1)) : Comparable<Luck> {
    var luck: Int = Random.nextInt(101)
        get() {
            if (counter.use()) {
                refreshLuck()
            }
            return field
        }

    fun refreshLuck() {
        luck = Random.nextInt(101)
        if (luck == 100) {
            while (Random.nextBoolean()) {
                luck++
            }
        }
        if (luck == 0) {
            while (Random.nextBoolean()) {
                luck--
            }
        }
    }

    override fun equals(other: Any?) = other is Luck && luck == other.luck

    override fun compareTo(other: Luck) = luck.compareTo(other.luck)

    override fun hashCode() = luck

    companion object {
        private val lucks = mutableMapStorageOf<Long, Luck>("data/luck.json") { Luck() }

        suspend fun get(id: Long) = lucks.referenceUpdate(id) { }.luck
    }
}

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandLuck() =
    literal("luck")
        .requiresPermission("command.luck", true)
        .executes {
            val luck = Luck.get(it.uid)
            val message = buildMessages {
                +"你今天的运气是: $luck"
                if (luck >= 100) {
                    +"！\n"
                    +"这运气也太好了吧！"
                }
                if (luck <= 0) {
                    +"！\n"
                    +"这运气也太差了吧......"
                }
            }
            it.reply(message)
        }