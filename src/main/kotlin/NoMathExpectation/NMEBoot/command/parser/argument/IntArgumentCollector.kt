package NoMathExpectation.NMEBoot.command.parser.argument

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.get
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect

class IntArgumentCollector<in S> : ArgumentCollector<S, Int> {
    override suspend fun collect(context: CommandContext<S>): Int {
        val str = context.reader.readWord() ?: error("期望一个int，但是什么都没有得到。")
        return when (str.take(2)) {
            "0x" -> str.drop(2).toIntOrNull(16) ?: error("无效的16进制int值 $str.")
            "0o" -> str.drop(2).toIntOrNull(8) ?: error("无效的8进制int值 $str.")
            "0b" -> str.drop(2).toIntOrNull(2) ?: error("无效的2进制int值 $str.")
            else -> str.toIntOrNull() ?: error("无效的int值 $str.")
        }
    }
}

fun <S> InsertableCommandNode<S>.collectInt(name: String) =
    collect(name, IntArgumentCollector())

class OptionalIntArgumentCollector<in S> : ArgumentCollector<S, Int?> {
    override suspend fun collect(context: CommandContext<S>): Int? {
        val str = context.reader.readWord() ?: return null
        return when (str.take(2)) {
            "0x" -> str.drop(2).toIntOrNull(16) ?: error("无效的16进制int值 $str.")
            "0o" -> str.drop(2).toIntOrNull(8) ?: error("无效的8进制int值 $str.")
            "0b" -> str.drop(2).toIntOrNull(2) ?: error("无效的2进制int值 $str.")
            else -> str.toIntOrNull() ?: error("无效的int值 $str.")
        }
    }
}

fun <S> InsertableCommandNode<S>.optionallyCollectInt(name: String) =
    collect(name, OptionalIntArgumentCollector())

fun CommandContext<*>.getInt(name: String) = get<Int>(name)