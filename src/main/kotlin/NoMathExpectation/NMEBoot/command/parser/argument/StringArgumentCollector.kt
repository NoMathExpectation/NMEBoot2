package NoMathExpectation.NMEBoot.command.parser.argument

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.get
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect

class StringArgumentCollector<in S> : ArgumentCollector<S, String> {
    override suspend fun collect(context: CommandContext<S>): String {
        val reader = context.reader
        reader.alignNextWord()

        val quoteChar = reader.peekChar() ?: error("期望一个字符串，但是什么都没有得到。")
        if (quoteChar !in quoteChars) {
            return reader.readWord() ?: error("期望一个字符串，但是什么都没有得到。")
        }

        reader.readChar()
        return buildString {
            var escaped = false
            while (true) {
                val read = reader.readChar()
                    ?: error("期望闭合符号'$quoteChar'，但是字符串未闭合。")

                if (escaped) {
                    require(read == quoteChar || read == escapeChar) { "无效的被转义符号'$read'。" }
                    append(read)
                    continue
                }

                if (read == escapeChar) {
                    escaped = true
                    continue
                }

                if (read == quoteChar) {
                    break
                }

                append(read)
            }
        }
    }

    private companion object {
        const val escapeChar = '\\'
        const val quoteChars = "\"'/"
    }
}

fun <S> InsertableCommandNode<S>.collectString(name: String) =
    collect(name, StringArgumentCollector())

class OptionalStringArgumentCollector<in S> : ArgumentCollector<S, String?> {
    override suspend fun collect(context: CommandContext<S>): String? {
        val reader = context.reader
        reader.alignNextWord()

        val quoteChar = reader.peekChar() ?: return null
        if (quoteChar !in quoteChars) {
            val word = reader.readWord()
            if (word == "null") {
                return null
            }
            return word
        }

        reader.readChar()
        return buildString {
            var escaped = false
            while (true) {
                val read = reader.readChar()
                    ?: error("期望闭合符号'$quoteChar'，但是字符串未闭合。")

                if (escaped) {
                    require(read == quoteChar || read == escapeChar) { "无效的被转义符号'$read'。" }
                    append(read)
                    continue
                }

                if (read == escapeChar) {
                    escaped = true
                    continue
                }

                if (read == quoteChar) {
                    break
                }

                append(read)
            }
        }
    }

    private companion object {
        const val escapeChar = '\\'
        const val quoteChars = "\"'/"
    }
}

fun <S> InsertableCommandNode<S>.optionallyCollectString(name: String) =
    collect(name, OptionalStringArgumentCollector())

class GreedyStringArgumentCollector<in S> : ArgumentCollector<S, String> {
    override suspend fun collect(context: CommandContext<S>): String {
        context.reader.alignNextWord()
        val str = context.reader.readRemain() ?: error("期望一个字符串，但是什么都没有得到。")
        return str
    }
}

fun <S> InsertableCommandNode<S>.collectGreedyString(name: String) =
    collect(name, GreedyStringArgumentCollector())

class OptionalGreedyStringArgumentCollector<in S> : ArgumentCollector<S, String?> {
    override suspend fun collect(context: CommandContext<S>): String? {
        context.reader.alignNextWord()
        val str = context.reader.readRemain()
        return str
    }
}

fun <S> InsertableCommandNode<S>.optionallyCollectGreedyString(name: String) =
    collect(name, OptionalGreedyStringArgumentCollector())

fun CommandContext<*>.getString(name: String) = get<String>(name)