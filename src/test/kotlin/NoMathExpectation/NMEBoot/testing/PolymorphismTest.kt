package NoMathExpectation.NMEBoot.testing

import NoMathExpectation.NMEBoot.util.polymorphicTransform
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertIs

class PolymorphismTest {
    interface I

    interface A : I

    class AImplA : A

    @Serializable
    @SerialName("A")
    class AImplB : A

    interface B : I

    class BImplA : B

    @Serializable
    @SerialName("B")
    class BImplB : B

    val module = SerializersModule {
        polymorphicTransform<I> {
            to<AImplB> {
                fromDelegate<A> { AImplB() }
            }
            to<BImplB> {
                fromDelegate<B> { BImplB() }
            }
        }
    }

    @Test
    fun test() {
        val json = Json { serializersModule = module }
        val map = mapOf(
            "A1" to AImplA(),
            "A2" to AImplB(),
            "B1" to BImplA(),
            "B2" to BImplB(),
        )
        val encoded = json.encodeToString(map)
        print(encoded)
        val decoded = json.decodeFromString<Map<String, I>>(encoded)
        assertIs<AImplB>(decoded["A1"])
        assertIs<AImplB>(decoded["A2"])
        assertIs<BImplB>(decoded["B1"])
        assertIs<BImplB>(decoded["B2"])
    }
}