package NoMathExpectation.NMEBoot.command.parser.argument

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.get
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect

class DoubleArgumentCollector<in S> : ArgumentCollector<S, Double> {
    override suspend fun collect(context: CommandContext<S>): Double {
        val str = context.reader.readWord() ?: error("期望一个double，但是什么都没有得到。")
        return str.toDoubleOrNull() ?: error("无效的double值 $str.")
    }
}

fun <S> InsertableCommandNode<S>.collectDouble(name: String) =
    collect(name, DoubleArgumentCollector())

class OptionalDoubleArgumentCollector<in S> : ArgumentCollector<S, Double?> {
    override suspend fun collect(context: CommandContext<S>): Double? {
        val str = context.reader.readWord() ?: return null
        return str.toDoubleOrNull() ?: error("无效的double值 $str.")
    }
}

fun <S> InsertableCommandNode<S>.optionallyCollectDouble(name: String) =
    collect(name, OptionalDoubleArgumentCollector())

fun CommandContext<*>.getDouble(name: String) = get<Double>(name)