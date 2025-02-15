package NoMathExpectation.NMEBoot.util

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.absoluteValue
import kotlin.math.round

// Copied from almighty ud2!
object BpmOffsetAnalyzer {
    private const val SECONDS_PER_MINUTE = 60.0

    private val logger = KotlinLogging.logger { }

    private fun interpolate(energy: List<Double>, offset: Double): Double {
        if (offset < 0.0) {
            return 0.0
        }
        return energy.getOrElse(offset.toInt()) { 0.0 }
    }

    private fun autoDifference(energy: List<Double>, offset: Double, interval: Double): Double {
        val pivot = interpolate(energy, offset)
        var difference = 0.0
        var total = 0.0

        listOf(1.0, 2.0, 4.0, 8.0, 16.0, 32.0).forEach {
            val current = interpolate(energy, offset + it * interval)
            val weight = 1.0 / it

            difference += weight * (current - pivot).absoluteValue
            total += weight
        }

        listOf(0.25, 0.5).forEach {
            val current = interpolate(energy, offset + it * interval)
            val weight = it

            difference -= weight * (current - pivot).absoluteValue
            total += weight
        }

        return difference / total
    }

    private fun accumulate(energy: List<Double>, offset: Double, interval: Double): Double {
        val samples = ((energy.size.toDouble() - offset) / interval).toLong()
        if (samples <= 0L) {
            return 0.0
        }

        return (0L until samples)
            .sumOf { interpolate(energy, offset + interval * it) } / samples
    }

    private fun averageBlur(span: List<Double>, radius: Long): List<Double> {
        return span.mapIndexed { index, s ->
            var sum = s
            (1..radius).forEach {
                sum += span[((index + it) % span.size).toInt()]
                sum += span[((index + span.lastIndex * it) % span.size).toInt()]
            }
            sum / (radius * 2 + 1)
        }
    }

    fun analyzeBpm(
        energy: List<Double>,
        rate: Double,
        min: Double = 84.0,
        max: Double = 146.0,
        steps: Long = 1024L,
        samples: Long = 1024L
    ): Double {
        if (samples <= 0) {
            return 0.0
        }

        val duration = energy.size.toDouble()
        val sample = duration / samples
        val slowest = SECONDS_PER_MINUTE / min * rate
        val fastest = SECONDS_PER_MINUTE / max * rate
        val step = if (steps <= 0) 0.0 else ((slowest - fastest) / steps)

        val interval = (0..steps)
            .map { fastest + step * it }
            .minBy { interval ->
                (0L until samples).sumOf {
                    autoDifference(energy, sample * it, interval)
                }
            }
        return SECONDS_PER_MINUTE / interval * rate
    }

    fun analyzeOffset(energy: List<Double>, rate: Double, bpm: Double, samples: Long = 1024L): Double {
        if (samples < 0L) {
            return 0.0
        }

        val beat = SECONDS_PER_MINUTE / bpm
        val interval = beat * rate
        val sample = interval / samples
        val blurred = averageBlur(List(samples.toInt()) {
            accumulate(energy, sample * it, interval)
        }, 8)

        val offset = blurred.withIndex().maxByOrNull {
            (0L until 20L).maxOf { index ->
                blurred[((it.index + index) % samples).toInt()]
            } - it.value
        }?.index?.toDouble() ?: 0.0
        return offset / samples * beat
    }

    @Serializable
    private data class Config(
        val ffmpeg: String = "ffmpeg",

        val interval: Int = 128,
        val sample: Long = 44100L,

        val min: Double = 56.0,
        val max: Double = 182.0,
    ) {
        val rate get() = sample.toDouble() / interval
    }

    private val configStorage = storageOf("config/offset_analyze.json", Config())

    private suspend fun measureEnergy(stream: InputStream): List<Double> {
        val config = configStorage.get()
        val result = mutableListOf<Double>()
        stream.use { inputStream ->
            var acc = 0.0
            buildList {
                val buffer = ByteBuffer.wrap(inputStream.readBytes())
                    .apply { order(ByteOrder.LITTLE_ENDIAN) }
                    .asFloatBuffer()
                while (buffer.hasRemaining()) {
                    add(buffer.get().absoluteValue.toDouble())
                }
            }.chunked(config.interval)
                .forEach { buffer ->
                    buffer.forEach { current ->
                        acc += (current - acc) / (if (current > acc) 8 else 512)
                    }
                    result += acc
                }
        }
        return result
    }

    private suspend fun getEnergy(input: File): List<Double> {
        val config = configStorage.get()
        val process = Runtime.getRuntime().exec(
            arrayOf(
                config.ffmpeg,
                "-v", "error",
                "-i", input.absolutePath,
                "-ar", config.sample.toString(),
                "-ac", "1",
                "-af", "lowpass=220",
                "-f", "f32le",
                "-"
            )
        )

        val result = measureEnergy(process.inputStream)
        if (process.waitFor() != 0) {
            throw RuntimeException(process.errorStream.reader().use { it.readText() }.trim())
        }
        return result
    }

    fun adjustBpm(bpm: Double): Double {
        val m = round(bpm * 6)
        val n = listOf(6, 1, 2, 3, 2, 1)[m.toInt() % 6]
        return if (n == 1) round(bpm) else m / n
    }

    suspend fun getBpmAndOffset(file: File, knownBpm: Double? = null): MusicBpmOffsetInfo =
        withContext(Dispatchers.IO) {
            val config = configStorage.get()
            logger.info { "开始测量文件${file.name}的bpm与偏移" }
            val energy = getEnergy(file)

            val bpm = knownBpm ?: analyzeBpm(
                energy,
                rate = config.rate,
                min = config.min,
                max = config.max
            )
            val adjustedBpm = knownBpm ?: adjustBpm(bpm)

            val offset = analyzeOffset(energy, rate = config.rate, bpm = adjustedBpm)

            return@withContext MusicBpmOffsetInfo(bpm, adjustedBpm, offset).also {
                logger.info { "测量完成: $it" }
            }
        }
}

data class MusicBpmOffsetInfo(
    val bpm: Double,
    val adjustedBpm: Double,
    val offset: Double,
)