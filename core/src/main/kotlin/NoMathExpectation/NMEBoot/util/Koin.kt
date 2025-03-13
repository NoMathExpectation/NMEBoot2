package NoMathExpectation.NMEBoot.util

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.ksp.generated.defaultModule

internal lateinit var koin: KoinApplication
    private set

internal fun startKoinApplication() {
    koin = startKoin {
        defaultModule()
    }
}