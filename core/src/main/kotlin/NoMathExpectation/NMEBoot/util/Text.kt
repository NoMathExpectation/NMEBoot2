package NoMathExpectation.NMEBoot.util

fun String.isEscapedAt(index: Int, escapeChar: Char = '\\'): Boolean {
    var backslashCount = 0
    var i = index - 1
    while (i >= 0 && this[i] == escapeChar) {
        backslashCount++
        i--
    }
    return backslashCount % 2 == 1
}

fun String.splitByUnescapedPaired(leftChar: Char, rightChar: Char, escapeChar: Char = '\\'): List<String> {
    val reader = StringReader(this)
    val result = mutableListOf<String>()

    while (!reader.isEnd) {
        val unpaired = reader.readUntilUnescaped(leftChar, false, escapeChar) ?: break
        val paired = reader.readUnescapedPaired(leftChar, rightChar, true, escapeChar)
        if (paired == null) {
            result += unpaired + (reader.readRemain() ?: "")
            break
        }

        if (unpaired.isNotEmpty()) {
            result += unpaired
        }
        result += paired
    }

    return result
}

fun String.splitUnescaped(separator: Char, escapeChar: Char = '\\'): List<String> {
    val reader = StringReader(this)
    val result = mutableListOf<String>()

    while (!reader.isEnd) {
        val part = reader.readUntilUnescaped(separator, false, escapeChar) ?: break
        result += part
        if (!reader.isEnd && reader.peekChar() == separator) {
            reader.readChar() // Skip the separator
        }
    }

    return result
}

fun String.unescapeLineFeeds() = buildString {
    val reader = StringReader(this@unescapeLineFeeds)
    while (!reader.isEnd) {
        val char = reader.readChar()
        if (char == '\\' && !reader.isEnd) {
            when (val nextChar = reader.readChar()) {
                'n' -> append('\n')
                'r' -> append('\r')
                else -> {
                    append(char)
                    append(nextChar)
                }
            }
        } else {
            append(char)
        }
    }
}