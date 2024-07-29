package NoMathExpectation.NMEBoot.util

import love.forte.simbot.definition.Member

val Member.nickOrName: String
    get() = nick ?: name