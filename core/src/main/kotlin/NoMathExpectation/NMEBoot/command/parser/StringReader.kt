package NoMathExpectation.NMEBoot.command.parser

import kotlin.math.min

class StringReader(
    val string: String,
    cursor: Int = 0,
) {
    var next = 0
        set(value) {
            require(value >= 0) { "Excepted a not negative cursor, but found $value." }
            field = value
        }

    init {
        next = cursor
    }

    fun copy() = StringReader(string, next)

    fun isEnd(index: Int) = index >= string.length

    val isEnd get() = isEnd(next)

    operator fun get(index: Int) = string.getOrNull(index)

    fun peekChar() = if (isEnd) null else string[next]

    fun readChar(): Char? {
        if (isEnd) {
            return null
        }
        return string[next++]
    }

    fun peekString(length: Int): String? {
        if (isEnd) {
            return null
        }
        val start = next
        val end = min(next + length, string.length)
        return string.substring(start, end)
    }

    fun readString(length: Int): String? {
        if (isEnd) {
            return null
        }
        val start = next
        val end = min(next + length, string.length)
        next = end
        return string.substring(start, end)
    }

    fun readRemain(): String? {
        if (isEnd) {
            return null
        }
        val start = next
        next = string.length
        return string.substring(start, next)
    }

    fun readUntil(char: Char): String? {
        if (isEnd) {
            return null
        }
        val start = next
        val end = string.indexOf(char, start)
        next = if (end == -1) string.length else end + 1
        return string.substring(start, next)
    }

    val nextWordIndex: Int? get() {
        var cur = next
        while (this[cur]?.isWhitespace() == true) {
            cur++
        }
        return if (isEnd(cur)) null else cur
    }

    val nextWordEndIndex: Int? get() {
        var cur = nextWordIndex ?: return null
        while (this[cur]?.isWhitespace() == false) {
            cur++
        }
        return cur
    }

    fun alignNextWord() {
        next = nextWordIndex ?: string.length
    }

    fun alignNextWordEnd() {
        next = nextWordEndIndex ?: string.length
    }

    fun peekWord(): String? {
        val start = nextWordIndex ?: return null
        val end = nextWordEndIndex ?: string.length
        return string.substring(start, end)
    }

    fun readWord(): String? {
        val start = nextWordIndex ?: return null
        val end = nextWordEndIndex ?: string.length
        next = end
        return string.substring(start, end)
    }
}