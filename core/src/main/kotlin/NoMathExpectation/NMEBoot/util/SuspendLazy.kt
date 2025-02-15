package NoMathExpectation.NMEBoot.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SuspendLazy<out T>(
    private val mutex: Mutex = Mutex(),
    private val compute: suspend () -> T,
) {
    private var cached: T? = null

    suspend fun get(): T = mutex.withLock {
        cached?.let { return it }
        compute().also { cached = it }
    }

    suspend operator fun invoke() = get()
}

fun <T> suspendLazy(compute: suspend () -> T) = SuspendLazy(compute = compute)