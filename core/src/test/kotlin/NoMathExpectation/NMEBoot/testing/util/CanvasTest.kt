package NoMathExpectation.NMEBoot.testing.util

import NoMathExpectation.NMEBoot.util.Canvas
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.test.Test

class CanvasTest {
    @Test
    fun testInstructions() {
        val inst = ">16 l p wdxa"
        val canvas = Canvas.createFromInstructions(inst)

        canvas.use {
            it.exportToStream().use {
                Path("data/temp/test_draw.png").writeBytes(it.toByteArray())
            }
        }
    }
}