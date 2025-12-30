package NoMathExpectation.NMEBoot.command.impl.command.rd

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.source.subjectHasPermission
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.on
import NoMathExpectation.NMEBoot.command.parser.node.overrideHelpCondition
import NoMathExpectation.NMEBoot.command.parser.node.reportOnFail
import NoMathExpectation.NMEBoot.util.toLocalDateTime
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val logger = KotlinLogging.logger { }
private const val message = "我六点就该下班了。"

@OptIn(ExperimentalTime::class)
fun InsertableCommandNode<AnyExecuteContext>.checkNotOverwork() = on(message) {
    val result = !it.executor.subjectHasPermission("command.rd.fanmade") || Clock.System.now()
        .toLocalDateTime().hour in 7..17 || Random.nextInt(100) != 0

    if (!result) {
        logger.info { message }
    }

    result
}.overrideHelpCondition { true }
    .reportOnFail()