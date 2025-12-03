package NoMathExpectation.NMEBoot.testing.util

import NoMathExpectation.NMEBoot.util.splitByUnescapedPaired
import NoMathExpectation.NMEBoot.util.splitUnescaped
import kotlin.test.Test
import kotlin.test.assertEquals

class TextTest {
    @Test
    fun testSplitPaired() {
        val expected = listOf("123", "[45\\]6:\\[7]", "89", "[0[12]3]", "4", "[]")
        val actual = "123[45\\]6:\\[7]89[0[12]3]4[]".splitByUnescapedPaired('[', ']')
        assertEquals(expected, actual)
    }

    @Test
    fun testSplitUnescaped() {
        val expected = listOf("part1", "part\\:2\\\\", "part3\\\\\\:part4")
        val actual = "part1:part\\:2\\\\:part3\\\\\\:part4".splitUnescaped(':')
        assertEquals(expected, actual)
    }
}