package NoMathExpectation.NMEBoot.testing.util

import NoMathExpectation.NMEBoot.util.canvas.Canvas
import kotlin.io.path.Path
import kotlin.io.path.writeBytes
import kotlin.test.Test

class CanvasTest {
    @Test
    fun testInstructions() {
        val inst = "s16 l p wdxa"
        val (canvas, _) = Canvas.createFromInstructions(inst)

        canvas.use {
            it.exportToStream().use {
                Path("data/temp/test_draw.png").writeBytes(it.toByteArray())
            }
        }
    }

    @Test
    fun testFibonacci() {
        val inst =
            "=qd=wd=aa=sa=i9=n[s4p&qp&ax=p@=e[@q@w]=d[@a@s]=q[@w]=w[@e]=a[@s]=s[@d]=p+-p]=f[=h[&n-is0&i&i&1]=1[ &h]&h]*f"
        val (canvas, _) = Canvas.createFromInstructions(inst)

        canvas.use {
            it.exportToStream().use {
                Path("data/temp/test_fibonacci.png").writeBytes(it.toByteArray())
            }
        }
    }
}