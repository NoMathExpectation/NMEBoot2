package NoMathExpectation.NMEBoot.command.parser.argument

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.get
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect

class LongArgumentCollector<in S> : ArgumentCollector<S, Long> {
    override suspend fun collect(context: CommandContext<S>): Long {
        val str = context.reader.readWord() ?: error("期望一个long，但是什么都没有得到。")
        return when (str.take(2)) {
            "0x" -> str.drop(2).toLongOrNull(16) ?: error("无效的16进制long值 $str.")
            "0o" -> str.drop(2).toLongOrNull(8) ?: error("无效的8进制long值 $str.")
            "0b" -> str.drop(2).toLongOrNull(2) ?: error("无效的2进制long值 $str.")
            else -> str.toLongOrNull() ?: error("无效的long值 $str.")
        }
    }
}

fun <S> InsertableCommandNode<S>.collectLong(name: String) =
    collect(name, LongArgumentCollector())

class OptionalLongArgumentCollector<in S> : ArgumentCollector<S, Long?> {
    override suspend fun collect(context: CommandContext<S>): Long? {
        val str = context.reader.readWord() ?: return null
        if (str.lowercase() == "null") {
            return null
        }
        return when (str.take(2)) {
            "0x" -> str.drop(2).toLongOrNull(16) ?: error("无效的16进制long值 $str.")
            "0o" -> str.drop(2).toLongOrNull(8) ?: error("无效的8进制long值 $str.")
            "0b" -> str.drop(2).toLongOrNull(2) ?: error("无效的2进制long值 $str.")
            else -> str.toLongOrNull() ?: error("无效的long值 $str.")
        }
    }
}

fun <S> InsertableCommandNode<S>.optionallyCollectLong(name: String) =
    collect(name, OptionalLongArgumentCollector())

fun CommandContext<*>.getLong(name: String) = get<Long>(name)