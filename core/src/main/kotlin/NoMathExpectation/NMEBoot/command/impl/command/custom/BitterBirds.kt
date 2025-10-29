package NoMathExpectation.NMEBoot.command.impl.command.custom

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.onMatchRegex
import NoMathExpectation.NMEBoot.command.parser.node.withMatchHelp

private val matchRegex = ".*真是一对苦命鸳鸯.*".toRegex()

// requested by AQ_ly, to be moved after implementation of //alias
suspend fun InsertableCommandNode<AnyExecuteContext>.bitterBirds() =
    requiresPermission("command.custom.bitter_birds")
        .onMatchRegex(matchRegex)
        .withMatchHelp("？？？")
        .executes("苦命鸳鸯") {
            it.reply("带着你的苦命鸳鸯吃大份去吧")
        }