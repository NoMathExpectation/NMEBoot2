package NoMathExpectation.NMEBoot.util

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

fun Random.sampleNormalDistribution(mean: Double = 0.0, stddev: Double = 1.0): Double {
    val u1 = nextDouble()
    val u2 = nextDouble()
    val z0 = sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)
    return z0 * stddev + mean
}