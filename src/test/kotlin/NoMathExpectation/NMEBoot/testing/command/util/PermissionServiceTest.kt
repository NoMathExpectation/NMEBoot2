package NoMathExpectation.NMEBoot.testing.command.util

import NoMathExpectation.NMEBoot.command.impl.PermissionService
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class PermissionServiceTest {
    @Test
    fun testPermission() = runTest {
        PermissionService.setPermission("test.foo.bar", "test-1", null)
        PermissionService.setPermission("test.foo", "test-1", true)

        assertTrue {
            PermissionService.hasPermission("test.foo.bar", "test-0", "test-1", "test-2")
        }
    }
}