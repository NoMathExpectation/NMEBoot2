package NoMathExpectation.NMEBoot.testing.util

import NoMathExpectation.NMEBoot.util.canvas.Canvas
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.test.Test

class CanvasTest {
    @Test
    fun testInstructions() {
        val inst = ">16 l p wdxa"
        val (canvas, _) = Canvas.createFromInstructions(inst)

        canvas.use {
            it.exportToStream().use {
                Path("data/temp/test_draw.png").writeBytes(it.toByteArray())
            }
        }
    }
}