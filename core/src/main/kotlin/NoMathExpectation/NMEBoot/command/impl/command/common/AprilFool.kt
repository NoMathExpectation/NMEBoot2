@file:OptIn(ExperimentalTime::class)

package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.command.parser.node.on
import NoMathExpectation.NMEBoot.message.message
import NoMathExpectation.NMEBoot.util.asMessages
import NoMathExpectation.NMEBoot.util.randomRemoveChars
import NoMathExpectation.NMEBoot.util.sampleNormalDistribution
import NoMathExpectation.NMEBoot.util.toLocalDateTime
import kotlinx.datetime.Month
import love.forte.simbot.event.InteractionMessage
import love.forte.simbot.event.InternalMessagePreSendEvent
import love.forte.simbot.message.PlainText
import love.forte.simbot.message.toMessages
import love.forte.simbot.message.toText
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private var deactivateUntil = Instant.DISTANT_PAST

private val shouldActivate: Boolean
    get() {
        val now = Clock.System.now()
        if (now < deactivateUntil) {
            return false
        }

        val currentDateTime = now.toLocalDateTime()
        return currentDateTime.month == Month.APRIL && currentDateTime.day == 1
    }

fun aprilFoolModifyMessage(event: InternalMessagePreSendEvent) {
    if (!shouldActivate) {
        return
    }

    val ratio = Random.sampleNormalDistribution(0.5, 0.1)
    event.currentMessage = InteractionMessage.valueOf(event.currentMessage.message.asMessages().map {
        if (it is PlainText) {
            it.text.randomRemoveChars(ratio).toText()
        } else {
            it
        }
    }.toMessages())
}

fun LiteralSelectionCommandNode<AnyExecuteContext>.commandAprilFool() = literal("aprilfool", "aprilfools")
    .on { shouldActivate }
    .executes {
        deactivateUntil = Clock.System.now() + 1.hours
        it.send("你隐约感觉神必力量暂时消失了...")
    }