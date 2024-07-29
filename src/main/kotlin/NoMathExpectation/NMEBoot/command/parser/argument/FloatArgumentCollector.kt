package NoMathExpectation.NMEBoot.command.parser.argument

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect

class FloatArgumentCollector<in S> : ArgumentCollector<S, Float> {
    override suspend fun collect(context: CommandContext<S>): Float {
        val str = context.reader.readWord() ?: error("期望一个float，但是什么都没有得到。")
        return str.toFloatOrNull() ?: error("无效的float值 $str.")
    }
}

fun <S> InsertableCommandNode<S>.collectFloat(name: String) =
    collect(name, FloatArgumentCollector())