package NoMathExpectation.NMEBoot.command

import NoMathExpectation.NMEBoot.util.storageOf
import kotlinx.serialization.Serializable

@Serializable
data class CommandConfig(
    var commandPrefix: String = "//",
)

val commandConfig = storageOf("config/command.json", CommandConfig())