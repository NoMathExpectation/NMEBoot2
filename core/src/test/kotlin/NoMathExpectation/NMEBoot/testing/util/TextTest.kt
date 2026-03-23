package NoMathExpectation.NMEBoot.testing.util

import NoMathExpectation.NMEBoot.util.randomRemoveChars
import NoMathExpectation.NMEBoot.util.splitByUnescapedPaired
import NoMathExpectation.NMEBoot.util.splitUnescaped
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.test.Test
import kotlin.test.assertEquals

private val logger = KotlinLogging.logger { }

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

    @Test
    fun testRemoveRandomChars() {
        val text = """
            你说得对，但是不要再Ultra了！Ultra是加拿大人研发的新型鸦片！加拿大人往你的电脑里安装大炮，当你Ultra时大炮就会被引燃，真是细思极恐！加拿大研发的机器人会自动生成Ultra图，不费任何人力就能让你的孩子上瘾！现在的孩子竟然打蔚蓝的Ultra图，可见加拿大人已经嘟害了中国青少年的心灵，你的孩子已经失去了炼金能力！Ultra图多有暴力元素，引导蔚批走向暴力，残害家人和朋友，让你的孩子有自残倾向！其实这些都是加拿大人的诡计！如果现在的青少年打的都是这种东西，以后我们的国家怎么会有栋梁之才！坚决抵制Ultra图，坚决抵制文化入侵！如果你认同我的看法，请转发出去，转告你的亲友，不要再Ultra了。抵制Ultra！！！
        """.trimIndent()

        logger.info { text.randomRemoveChars(0.4) }
    }
}