package NoMathExpectation.NMEBoot.command.parser.argument

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.get
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect

class BooleanArgumentCollector<in S> : ArgumentCollector<S, Boolean> {
    override suspend fun collect(context: CommandContext<S>): Boolean {
        val str = context.reader.readWord() ?: error("期望一个布尔值，但是什么都没有得到。")
        return when (str.lowercase()) {
            "0", "false", "假", "否", "不" -> false
            "1", "true", "真", "是", "是的" -> true
            else -> error("无效的布尔值 $str.")
        }
    }

    override fun buildHelp(name: String) = "<$name:bool>"
}

fun <S> InsertableCommandNode<S>.collectBoolean(name: String) =
    collect(name, BooleanArgumentCollector())

class OptionalBooleanArgumentCollector<in S> : ArgumentCollector<S, Boolean?> {
    override suspend fun collect(context: CommandContext<S>): Boolean? {
        val str = context.reader.readWord() ?: return null
        return when (str.lowercase()) {
            "0", "false", "假", "否", "不" -> false
            "1", "true", "真", "是", "是的" -> true
            "null", "nil", "undefined" -> null
            else -> error("无效的布尔值 $str.")
        }
    }

    override fun buildHelp(name: String) = "[$name:bool]"
}

fun <S> InsertableCommandNode<S>.optionallyCollectBoolean(name: String) =
    collect(name, OptionalBooleanArgumentCollector())

fun CommandContext<*>.getBoolean(name: String) = get<Boolean>(name)