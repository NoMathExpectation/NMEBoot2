package NoMathExpectation.NMEBoot.util.canvas

import NoMathExpectation.NMEBoot.util.canvas.Canvas.Companion.DEFAULT_STEP
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

sealed interface CanvasInstruction

data object NoOpInstruction : CanvasInstruction

data object TogglePenInstruction : CanvasInstruction

data class ToggleColorInstruction(val color: Color) : CanvasInstruction

data class ToggleDrawModeInstruction(val mode: DrawMode) : CanvasInstruction

data class SetStepInstruction(val step: Int) : CanvasInstruction

data class MovePenInstruction(val direction: Char) : CanvasInstruction

data class InstructParseException(val reason: String, val relevantInstruction: String) :
    IllegalStateException("解析指令时发生了错误：$reason\n...$relevantInstruction...")

class InstructionReader(val instruction: String) {
    private val logger = KotlinLogging.logger { }

    var unwindInstruction = instruction
    var cursor = 0

    val cursorChar get() = unwindInstruction[cursor]

    private val variables = mutableMapOf(
        "r" to Random.nextInt().toString(),
        "p" to "*",
        "l" to "&",
    )

    var random: Random = Random(variables["r"]!!.toInt())

    fun throwParseException(reason: String): Nothing {
        throw InstructParseException(
            reason,
            unwindInstruction.substring(max(cursor - 10, 0), min(cursor + 10, unwindInstruction.length))
        )
    }

    val pointerSymbol get() = getVariable("p").lastOrNull() ?: throwParseException("缺失强制引用标识符。")
    val lazyPointerSymbol get() = getVariable("l").lastOrNull() ?: throwParseException("缺失懒惰引用标识符。")

    private fun dereferencePointer() {
        if (cursorChar !in listOf(pointerSymbol, lazyPointerSymbol)) {
            throwParseException("尝试解引用一个非指针的值。")
        }
        val startCursor = cursor
        cursor++
        val varName = readCharOrPaired(true)
        val varValue = getVariable(varName)
        unwindInstruction = unwindInstruction.substring(0, startCursor) + varValue + unwindInstruction.substring(cursor)
        cursor = startCursor
    }

    private fun peekChar(derefLazyPtr: Boolean): Char? {
        if (cursor >= unwindInstruction.length) {
            return null
        }

        val deref = (derefLazyPtr && (cursorChar in listOf(
            pointerSymbol,
            lazyPointerSymbol
        ))) || (!derefLazyPtr && cursorChar == pointerSymbol)
        if (deref) {
            dereferencePointer()
            return peekChar(derefLazyPtr)
        }

        return cursorChar
    }

    private fun readCharOrNull(derefLazyPtr: Boolean): Char? {
        return peekChar(derefLazyPtr)?.also { cursor++ }
    }

    private fun readChar(derefLazyPtr: Boolean): Char {
        return (peekChar(derefLazyPtr) ?: throwParseException("期望更多字符，但是已经到达了指令结尾。")).also { cursor++ }
    }

    private fun peekString(length: Int, derefLazyPtr: Boolean): String? = buildString {
        repeat(length) {
            val char = readCharOrNull(derefLazyPtr) ?: run {
                cursor -= it + 1
                return null
            }
            append(char)
        }
        cursor -= length
    }

    private fun readString(length: Int, derefLazyPtr: Boolean): String {
        val string =
            peekString(length, derefLazyPtr) ?: throwParseException("期望长度为${length}的字符，但是已经到达了指令结尾。")
        return string.also { cursor += length }
    }

    val pairChars = mapOf(
        '(' to ')',
        '[' to ']',
        '{' to '}',
    )

    private fun readPaired(derefLazyPtr: Boolean): String {
        val char = readChar(derefLazyPtr)
        if (char !in pairChars) {
            throwParseException("尝试读取括号内容，但是当前字符${char}不是括号。")
        }

        val pairChar = pairChars[char]
        return buildString {
            var depth = 1

            while (true) {
                val c = readChar(derefLazyPtr)
                if (c == char) {
                    depth++
                } else if (c == pairChar) {
                    depth--
                    if (depth <= 0) {
                        break
                    }
                }
                append(c)
            }
        }
    }

    private fun readCharOrPaired(derefLazyPtr: Boolean): String {
        val char = peekChar(derefLazyPtr) ?: throwParseException("期望更多字符，但是已经到达了指令结尾。")
        if (char in pairChars) {
            return readPaired(derefLazyPtr)
        }

        return char.toString().also { cursor++ }
    }

    private fun readNumberString(): String = buildString {
        if (peekChar(true)?.isDigit() != true) {
            if (peekChar(true) == '-') {
                append('-')
                cursor++
            } else {
                throwParseException("期望一个数字，但是当前字符不是数字。")
            }
        }

        while (true) {
            val char = peekChar(true) ?: break
            if (char.isDigit()) {
                append(char)
                cursor++
            } else {
                break
            }
        }
    }

    private fun readInt(): Int {
        val str = readNumberString()
        return str.toIntOrNull() ?: throwParseException("无效的整数：$str")
    }

    private fun readColor(): Color {
        if (cursor >= unwindInstruction.length) {
            throwParseException("期望更多字符，但是已经到达了指令结尾。")
        }

        if (cursorChar in pairChars) {
            val colorStr = readPaired(true)
            return runCatching {
                Color(
                    colorStr.toInt(),
                    true,
                )
            }.getOrElse {
                throwParseException("无效的颜色字符串：$colorStr")
            }
        }

        val colorStr = readString(8, true)
        return runCatching {
            colorStr.toLong(16)
                .toInt()
                .let {
                    Color(
                        it,
                        true,
                    )
                }
        }.getOrElse {
            throwParseException("无效的颜色字符串：$colorStr")
        }
    }

    private fun setVariable(name: String, value: String) {
        if (name == RANDOM_VARIABLE_NAME) {
            random = Random(value.toIntOrNull() ?: value.hashCode())
        }

        variables[name] = value
    }

    private fun getVariable(name: String): String {
        if (name == RANDOM_VARIABLE_NAME) {
            return random.nextInt().toString()
        }

        return variables[name] ?: "0"
    }

    private fun addValues(left: String, right: String): String {
        left.toIntOrNull()?.let { l ->
            right.toIntOrNull()?.let { r ->
                return (l + r).toString()
            }
        }

        return left + right
    }

    private fun minusValues(left: String, right: String): String {
        val l = left.toIntOrNull()
        val r = right.toIntOrNull()

        if (l != null && r != null) {
            return (l - r).toString()
        }

        if (r != null) {
            return left.dropLast(r)
        }

        throwParseException("无效的减法运算：$left 和 $right")
    }

    private fun multiplyValues(left: String, right: String): String {
        val l = left.toIntOrNull()
        val r = right.toIntOrNull()

        if (l != null && r != null) {
            return (l * r).toString()
        }

        if (r != null) {
            return if (r > 0) {
                left.repeat(r)
            } else if (r == 0) {
                ""
            } else {
                left.reversed().repeat(-r)
            }
        }

        if (l != null) {
            return if (l > 0) {
                right.repeat(l)
            } else if (l == 0) {
                ""
            } else {
                right.reversed().repeat(-l)
            }
        }

        throwParseException("无效的乘法运算：$left 和 $right")
    }

    private fun divideValues(left: String, right: String): String {
        val l = left.toIntOrNull() ?: throwParseException("尝试对非整数值进行除法运算：$left")
        val r = right.toIntOrNull() ?: throwParseException("尝试对非整数值进行除法运算：$right")
        if (r == 0) {
            throwParseException("尝试对0进行除法运算。")
        }
        return (l / r).toString()
    }

    private fun modValues(left: String, right: String): String {
        val l = left.toIntOrNull() ?: throwParseException("尝试对非整数值进行取模运算：$left")
        val r = right.toIntOrNull() ?: throwParseException("尝试对非整数值进行取模运算：$right")
        if (r == 0) {
            throwParseException("尝试对0进行取模运算。")
        }
        return (l % r).toString()
    }

    private fun doBinaryOp(leftVarName: String, rightValue: String, op: Char) {
        val left = getVariable(leftVarName)
        val result = when (op) {
            '+' -> addValues(left, rightValue)
            '-' -> minusValues(left, rightValue)
            '*' -> multiplyValues(left, rightValue)
            '/' -> divideValues(left, rightValue)
            '%' -> modValues(left, rightValue)
            else -> throwParseException("无效的二元运算符：$op")
        }
        setVariable(leftVarName, result)
    }

    private fun unaryPlus(value: String): String {
        value.toIntOrNull()?.let {
            return (it + 1).toString()
        }

        if (value.length == 1) {
            return (value[0].code + 1).toChar().toString()
        }

        throwParseException("无法对${value}自增")
    }

    private fun unaryMinus(value: String): String {
        value.toIntOrNull()?.let {
            return (it - 1).toString()
        }

        if (value.length == 1) {
            return (value[0].code - 1).toChar().toString()
        }

        return value.dropLast(1)
    }

    private fun doUnaryOp(varName: String, op: Char) {
        val value = getVariable(varName)
        val result = when (op) {
            '+' -> unaryPlus(value)
            '-' -> unaryMinus(value)
            else -> throwParseException("无效的单目运算符：$op")
        }
        setVariable(varName, result)
    }

    fun readNext(): CanvasInstruction? {
        return when (val char = readCharOrNull(true)) {
            null -> null
            'q', 'w', 'e', 'a', 'd', 'z', 'x', 'c' -> MovePenInstruction(char)
            'p' -> TogglePenInstruction
            '#' -> ToggleColorInstruction(readColor())
            'l' -> ToggleDrawModeInstruction(DrawMode.LINE)
            'f' -> ToggleDrawModeInstruction(DrawMode.FILL)
            's' -> SetStepInstruction(readInt())
            '=' -> {
                val varName = readCharOrPaired(true)
                val varValue = readCharOrPaired(false)
                setVariable(varName, varValue)
                NoOpInstruction
            }

            '+', '-' -> {
                val varName = readCharOrPaired(true)
                doUnaryOp(varName, char)
                NoOpInstruction
            }

//            '+', '-', '*', '/', '%' -> {
//                val leftName = readCharOrPaired(true)
//                val right = readCharOrPaired(false)
//                doBinaryOp(leftName, right, char)
//                NoOpInstruction
//            }

            '(', '[', '{' -> {
                cursor--
                readCharOrPaired(false)
                NoOpInstruction
            }

            pointerSymbol, lazyPointerSymbol -> {
                // 你是怎么进到这里的？
                logger.warn { "在${cursor}处读到了解引用符号'$char'！\n相关指令：$unwindInstruction" }
                NoOpInstruction
            }

            ' ' -> NoOpInstruction
            else -> throwParseException("无效的指令字符：$char")
        }
    }

    fun variablesToString(): String {
        return variables.entries.joinToString("\n") { (k, v) -> "$k=\"$v\"" }
    }

    companion object {
        const val MAX_INSTRUCTIONS = 1_000_000
        const val RANDOM_VARIABLE_NAME = "r"

        val INST_PATTERN_INFO = """
            qweadzxc：八个方向移动画笔
            p：切换画笔状态（落笔/抬笔）
            #AARRGGBB：设置颜色（16进制ARGB）
            #[num]：设置颜色（10进制整数ARGB）
            l：设置绘制模式为线条模式
            f：设置绘制模式为填充模式
            sN：设置步长为N（N为整数，默认为$DEFAULT_STEP）
            =xc：将变量x的值设为c
            +x：使变量x自增
            -x：使变量x自减
            (...)，[]，{}：将括号里的内容视为一个整体
            &x：懒惰变量引用，只有在需要的时候才会解引用，引用符号取变量l的末尾
            *x：强制变量引用，无条件解引用成变量的值，引用符号取变量p的末尾
            空格：无操作（可用于分隔指令）
            指令区分大小写，其他字符均视为无效指令
            从变量${RANDOM_VARIABLE_NAME}中读取随机数，赋值给${RANDOM_VARIABLE_NAME}以设置种子
        """.trimIndent()

        /*  unused
        *   +xc：使变量x的值加上c
        *   -xc：使变量x的值减去c
        *   *xc：使变量x的值乘以c
        *   /xc：使变量x的值除以c（整数除法）
        *   %xc：使变量x的值对c取模
        */
    }
}

fun Canvas.runInstructions(inst: String): InstructionReader {
    val reader = InstructionReader(inst)

    var cnt = 0
    while (true) {
        when (val instruction = reader.readNext() ?: break) {
            is NoOpInstruction -> {
                // no op
            }

            is TogglePenInstruction -> penDown = !penDown
            is ToggleColorInstruction -> this.color = instruction.color
            is ToggleDrawModeInstruction -> this.drawMode = instruction.mode
            is SetStepInstruction -> this.step = instruction.step
            is MovePenInstruction -> movePen(instruction.direction)
        }

        if (++cnt > InstructionReader.MAX_INSTRUCTIONS) {
            reader.throwParseException("指令执行次数超过了上限。")
        }
    }

    return reader
}