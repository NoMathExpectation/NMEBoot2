package NoMathExpectation.NMEBoot.util.canvas

import love.forte.simbot.message.Image
import love.forte.simbot.message.OfflineImage.Companion.toOfflineImage
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

enum class DrawMode {
    LINE, FILL
}

abstract class Canvas : AutoCloseable {
    abstract val width: Int
    abstract val height: Int

    abstract var color: Color
    fun setColor(rgbaHex: String) {
        runCatching {
            color = Color(rgbaHex.toLong(16).toInt(), true)
        }.onFailure {
            error("无效的颜色代码：$rgbaHex")
        }
    }

    var penX = width / 2
    var penY = height / 2
    var step = DEFAULT_STEP
    var drawMode = DrawMode.LINE

    abstract fun movePen(dir: Char)

    open var penDown = false

    abstract fun exportToStream(format: String = "png"): ByteArrayOutputStream

    fun exportToImage(format: String = "png"): Image {
        return exportToStream(format).use { it.toByteArray().toOfflineImage() }
    }

    companion object {
        const val DEFAULT_STEP = 16

        private data class Border(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int)

        private fun calculateBorder(instructions: String, startX: Int = 0, startY: Int = 0): Border {
            val fakeCanvas = FakeCanvas().apply {
                penX = startX
                penY = startY
            }
            fakeCanvas.runInstructions(instructions)
            return Border(fakeCanvas.minX, fakeCanvas.minY, fakeCanvas.maxX, fakeCanvas.maxY)
        }

        fun createFromInstructions(inst: String): Pair<RealCanvas, InstructionReader> {
            val border = calculateBorder(inst)
            val canvasWidth = (border.maxX.toLong() - border.minX.toLong() + 1).coerceAtMost(8192) + 16
            val canvasHeight = (border.maxY.toLong() - border.minY.toLong() + 1).coerceAtMost(8192) + 16
            val canvas = RealCanvas(canvasWidth.toInt(), canvasHeight.toInt())
            canvas.penX = -border.minX + 8
            canvas.penY = -border.minY + 8

            val reader = canvas.runInstructions(inst)

            return canvas to reader
        }
    }
}

class FakeCanvas : Canvas() {
    var minX = penX
        private set
    var maxX = penX
        private set
    var minY = penY
        private set
    var maxY = penY
        private set

    override val width: Int
        get() = maxX - minX + 1
    override val height: Int
        get() = maxY - minY + 1

    override var color: Color = Color.BLACK

    override fun movePen(dir: Char) {
        when (dir) {
            'w' -> penY -= step
            'a' -> penX -= step
            'x' -> penY += step
            'd' -> penX += step
            'q' -> {
                penX -= step
                penY -= step
            }

            'e' -> {
                penX += step
                penY -= step
            }

            'z' -> {
                penX -= step
                penY += step
            }

            'c' -> {
                penX += step
                penY += step
            }

            else -> throw IllegalArgumentException("无效的方向：$dir")
        }

        if (penDown) {
            minX = minOf(minX, penX)
            maxX = maxOf(maxX, penX)
            minY = minOf(minY, penY)
            maxY = maxOf(maxY, penY)
        }
    }

    override fun exportToStream(format: String): ByteArrayOutputStream {
        throw UnsupportedOperationException("FakeCanvas does not support exporting to stream.")
    }

    override fun close() {}
}

class RealCanvas(override val width: Int = 256, override val height: Int = 256) : Canvas() {
    private val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    private val graphics = image.createGraphics()

    override fun close() {
        graphics.dispose()
    }

    override var color: Color
        get() = graphics.color
        set(value) {
            graphics.color = value
        }

    init {
        setColor("ff000000")
    }

    private val cachedPoints: MutableList<Pair<Int, Int>> = mutableListOf()

    override fun movePen(dir: Char) {
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

            else -> throw IllegalArgumentException("无效的方向：$dir")
        }
        if (penDown) {
            cachedPoints.add(penX to penY)
        }
    }

    override var penDown = false
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

    override fun exportToStream(format: String): ByteArrayOutputStream {
        penDown = false
        val stream = ByteArrayOutputStream()
        ImageIO.write(image, format, stream)
        return stream
    }
}

