package NoMathExpectation.NMEBoot.command.impl.command.rd

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.onMatchRegex
import NoMathExpectation.NMEBoot.command.parser.node.withMatchHelp

private val matchRegex = ".*林孙.*".toRegex()

// requested by 林孙, to be moved after implementation of //alias
suspend fun InsertableCommandNode<AnyExecuteContext>.linSunForCat() =
    requiresPermission("command.rd.cat", silent = true)
        .onMatchRegex(matchRegex)
        .withMatchHelp("...林孙...")
        .executes("猫猫") {
            it.reply("猫猫")
        }