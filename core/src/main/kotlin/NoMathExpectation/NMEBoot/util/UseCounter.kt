@file:OptIn(ExperimentalTime::class)

package NoMathExpectation.NMEBoot.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Serializable
sealed class UseCounter() {
    abstract var useCount: Int

    @Suppress("LeakingThis")
    var remain = useCount

    open fun use() = canUse().also {
        if (it) remain--
    }

    open fun canUse() = remain > 0

    open fun reset() {
        remain = useCount
    }

    companion object {
        operator fun invoke(useCount: Int) = LimitedTimeUseCounter(useCount)
    }
}

interface TimeRefreshable {
    var nextRefresh: LocalDateTime

    val timeUntilRefresh: Duration
        get() = nextRefresh.toInstant() - Clock.System.now()
}

@Serializable
@SerialName("limited-time-use-counter")
class LimitedTimeUseCounter(override var useCount: Int) : UseCounter()

@Serializable
@SerialName("fixed-delay-use-counter")
class FixedDelayUseCounter(
    override var useCount: Int,
    val delay: Duration,
    override var nextRefresh: LocalDateTime = Instant.DISTANT_PAST.toLocalDateTime()
) : UseCounter(), TimeRefreshable {
    override fun use() = canUse().also {
        if (it) remain--
    }

    override fun canUse(): Boolean {
        val now = Clock.System.now()
        if (now >= nextRefresh.toInstant()) {
            nextRefresh = (now + delay).toLocalDateTime()
            reset()
        }
        return super.canUse()
    }
}

@Serializable
@SerialName("fixed-rate-use-counter")
class FixedRateUseCounter(
    override var useCount: Int,
    val rate: Duration,
    override var nextRefresh: LocalDateTime = Clock.System.now().toLocalDateTime()
) : UseCounter(), TimeRefreshable {
    override fun use() = canUse().also {
        if (it) remain--
    }

    override fun canUse(): Boolean {
        val now = Clock.System.now().toLocalDateTime()
        if (now >= nextRefresh) {
            while (nextRefresh <= now) {
                nextRefresh = (nextRefresh.toInstant() + rate).toLocalDateTime()
            }
            reset()
        }
        return super.canUse()
    }

    companion object {
        fun ofDay(useCount: Int): FixedRateUseCounter {
            val now = Clock.System.now().toLocalDateTime()
            return FixedRateUseCounter(
                useCount,
                Duration.parse("1d"),
                LocalDateTime(now.year, now.month, now.day, 0, 0, 0, 0)
            )
        }
    }
}

internal fun Instant.toLocalDateTime() = toLocalDateTime(TimeZone.currentSystemDefault())

internal fun LocalDateTime.toInstant() = toInstant(TimeZone.currentSystemDefault())