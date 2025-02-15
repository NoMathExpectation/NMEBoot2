package NoMathExpectation.NMEBoot.testing.util

import NoMathExpectation.NMEBoot.util.mutableMapStorageOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StorageTest {
    val storage = mutableMapStorageOf<Int, String>("data/temp/test_storage.json") { "" }

    @Test
    fun test() = runTest {
        storage.clear()
        assertTrue(storage.get(1).isEmpty())

        storage.set(1, "Hello")
        assertEquals("Hello", storage.get(1))

        storage.update(1) { "$it, world!" }
        assertEquals("Hello, world!", storage.get(1))

        storage.referenceUpdate(1) {}
    }
}