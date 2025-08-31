package NoMathExpectation.NMEBoot.util

import love.forte.simbot.message.Image
import love.forte.simbot.message.OfflineImage.Companion.toOfflineImage
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.lang.AutoCloseable
import javax.imageio.ImageIO

class Canvas(val width: Int = 256, val height: Int = 256) : AutoCloseable {
    enum class DrawMode {
        LINE, FILL
    }

    private val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    private val graphics = image.createGraphics()

    override fun close() {
        graphics.dispose()
    }

    var color: Color
        get() = graphics.color
        set(value) {
            graphics.color = color
        }

    init {
        setColor("ff000000")
    }

    fun setColor(rgbaHex: String) {
        runCatching {
            graphics.color = Color(rgbaHex.toLong(16).toInt(), true)
        }.onFailure {
            error("无效的颜色代码：$rgbaHex")
        }
    }

    var penX = width / 2
    var penY = height / 2
    var step = DEFAULT_STEP
    var drawMode = DrawMode.LINE
    private val cachedPoints: MutableList<Pair<Int, Int>> = mutableListOf()

    fun movePen(dir: Char) {
        when (dir) {
            'w' -> penY = (penY - step).coerceIn(0, height - 1)
            'a' -> penX = (penX - step).coerceIn(0, width - 1)
            'x' -> penY = (penY + step).coerceIn(0, height - 1)
            'd' -> penX = (penX + step).coerceIn(0, width - 1)
            'q' -> {
                penX = (penX - step).coerceIn(0, width - 1)
                penY = (penY - step).coerceIn(0, height - 1)
            }

            'e' -> {
                penX = (penX + step).coerceIn(0, width - 1)
                penY = (penY - step).coerceIn(0, height - 1)
            }

            'z' -> {
                penX = (penX - step).coerceIn(0, width - 1)
                penY = (penY + step).coerceIn(0, height - 1)
            }

            'c' -> {
                penX = (penX + step).coerceIn(0, width - 1)
                penY = (penY + step).coerceIn(0, height - 1)
            }

            's' -> return
            else -> throw IllegalArgumentException("无效的方向：$dir")
        }
        if (penDown) {
            cachedPoints.add(penX to penY)
        }
    }

    var penDown = false
        set(value) {
            if (field != value) {
                if (value) {
                    penDown()
                } else {
                    penUp()
                }
            }
            field = value
        }

    private fun penDown() {
        cachedPoints += penX to penY
    }

    private fun penUp() {
        if (cachedPoints.size <= 1) {
            cachedPoints.clear()
            return
        }

        when (drawMode) {
            DrawMode.LINE -> {
                for (i in 0 until cachedPoints.size - 1) {
                    val (x1, y1) = cachedPoints[i]
                    val (x2, y2) = cachedPoints[i + 1]
                    graphics.drawLine(x1, y1, x2, y2)
                }
            }

            DrawMode.FILL -> {
                val xArray = cachedPoints.map { it.first }.toIntArray()
                val yArray = cachedPoints.map { it.second }.toIntArray()
                graphics.fillPolygon(xArray, yArray, cachedPoints.size)
            }
        }

        cachedPoints.clear()
    }

    fun exportToStream(format: String = "png"): ByteArrayOutputStream {
        penDown = false
        val stream = ByteArrayOutputStream()
        ImageIO.write(image, format, stream)
        return stream
    }

    fun exportToImage(format: String = "png"): Image {
        return exportToStream(format).use { it.toByteArray().toOfflineImage() }
    }

    companion object {
        const val MAX_INSTRUCTIONS = 100_000
        const val DEFAULT_STEP = 16

        val INST_PATTERN_INFO = """
            qweadzxc：八个方向移动画笔
            p：切换画笔状态（落笔/抬笔）
            #AARRGGBB：设置颜色（16进制ARGB）
            l：设置绘制模式为线条模式
            f：设置绘制模式为填充模式
            >N：设置步长为N（N为整数，默认为$DEFAULT_STEP）
            空格：无操作（可用于分隔指令）
            指令区分大小写，其他字符均视为无效指令
        """.trimIndent()

        private data class Border(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

        private fun calculateBorder(instructions: String, startX: Int = 0, startY: Int = 0): Border {
            var x = startX
            var y = startY
            var minX = x
            var maxX = x
            var minY = y
            var maxY = y

            var step = DEFAULT_STEP

            var penDown = false

            val reader = StringReader(instructions)
            while (true) {
                when (val char = reader.readChar() ?: break) {
                    'w' -> y -= step
                    'a' -> x -= step
                    'x' -> y += step
                    'd' -> x += step
                    'q' -> {
                        x -= step
                        y -= step
                    }

                    'e' -> {
                        x += step
                        y -= step
                    }

                    'z' -> {
                        x -= step
                        y += step
                    }

                    'c' -> {
                        x += step
                        y += step
                    }

                    'p' -> penDown = !penDown
                    '>' -> step = reader.readNumberString()?.toIntOrNull() ?: error("无效的步长数字")
                }

                if (penDown) {
                    minX = minOf(minX, x)
                    maxX = maxOf(maxX, x)
                    minY = minOf(minY, y)
                    maxY = maxOf(maxY, y)
                }
            }

            return Border(minX, minY, maxX, maxY)
        }

        fun createFromInstructions(inst: String): Canvas {
            val border = calculateBorder(inst)
            val canvasWidth = (border.maxX - border.minX + 1).coerceAtMost(8192) + 16
            val canvasHeight = (border.maxY - border.minY + 1).coerceAtMost(8192) + 16
            val canvas = Canvas(canvasWidth, canvasHeight)
            canvas.penX = -border.minX + 8
            canvas.penY = -border.minY + 8

            val reader = StringReader(inst)
            while (true) {
                when (val char = reader.readChar() ?: break) {
                    'p' -> canvas.penDown = !canvas.penDown
                    '#' -> {
                        val rgbaHex = reader.readString(8) ?: error("颜色代码位数不足")
                        canvas.setColor(rgbaHex)
                    }

                    'l' -> canvas.drawMode = DrawMode.LINE
                    'f' -> canvas.drawMode = DrawMode.FILL

                    '>' -> canvas.step = reader.readNumberString()?.toIntOrNull() ?: error("无效的步长数字")

                    'q', 'w', 'e', 'a', 's', 'd', 'z', 'x', 'c' -> {
                        canvas.movePen(char)
                    }

                    ' ' -> {}
                    else -> error("无效的指令字符：$char")
                }
            }

            return canvas
        }
    }
}