package NoMathExpectation.NMEBoot.command.parser.argument

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.node.ArgumentCollectCommandNode

class RangeCheckArgumentCollector<in S, out T : Comparable<T & Any>?>(
    val collector: ArgumentCollector<S, T>,
    val min: T? = null,
    val max: T? = null,
) : ArgumentCollector<S, T> {
    private fun throwOutOfRange(value: T): Nothing {
        when {
            min != null && max != null -> error("期望一个介于 $min 和 $max 之间的值，但是得到了 $value")
            min != null -> error("期望一个大于等于 $min 的值，但是得到了 $value")
            max != null -> error("期望一个小于等于 $max 的值，但是得到了 $value")
            else -> error("你是怎么触发这个的？")
        }
    }

    override suspend fun collect(context: CommandContext<S>): T {
        val value = collector.collect(context)
        if (value == null) return value
        if (min != null && value < min) throwOutOfRange(value)
        if (max != null && value > max) throwOutOfRange(value)
        return value
    }

    override fun buildHelp(name: String) = "${collector.buildHelp(name)}($min-$max)"
}

fun <S, T : Comparable<T & Any>?> ArgumentCollectCommandNode<S, T>.checkInRange(min: T? = null, max: T? = null) =
    apply { collector = RangeCheckArgumentCollector(collector, min, max) }