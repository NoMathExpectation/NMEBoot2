package NoMathExpectation.NMEBoot.command.impl

import NoMathExpectation.NMEBoot.command.impl.command.admin.CooldownConfig
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.onNotFail
import NoMathExpectation.NMEBoot.command.parser.node.reportOnFail
import NoMathExpectation.NMEBoot.util.TimeRefreshable

class CooldownException(message: String) : CommandParseException(message)

private suspend fun <T> CommandSource<T>.checkCooldown() {
    if (hasPermission("bypass.cooldown")) {
        return
    }

    val msg = CooldownConfig.storage.referenceUpdate {
        val timer = globalSubjectPermissionId?.let { id ->
            it.getGroupUser(id, uid)
        } ?: it.getPrivateUser(uid)

        if (timer.use()) {
            return@referenceUpdate null
        }

        if (timer is TimeRefreshable) {
            "请等待${timer.timeUntilRefresh.inWholeSeconds}s后使用指令"
        } else {
            "指令使用次数已用完"
        }
    }

    msg?.let {
        throw CooldownException(it)
    }
}

fun InsertableCommandNode<AnyExecuteContext>.consumeCooldown() = onNotFail {
    if (it.bypassCooldown) {
        return@onNotFail
    }
    it.executor.checkCooldown()
}.reportOnFail()