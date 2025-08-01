package NoMathExpectation.NMEBoot.scripting

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

private val logger = KotlinLogging.logger { }

fun main() {
    logger.info { "Preheating eval service..." }
    val eval = "print(\"Hello, world!\")".toSimpleEval()
    eval()
    logger.info { eval.output }

    embeddedServer(CIO, port = 8080) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(CallLogging)

        routing {
            evalService()
        }
    }.start(wait = true)
}