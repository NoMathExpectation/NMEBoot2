package NoMathExpectation.NMEBoot.command.parser.argument.ext

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.argument.ArgumentCollector
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect

class PermissionIdArgumentCollector : ArgumentCollector<AnyExecuteContext, String> {
    override suspend fun collect(context: CommandContext<AnyExecuteContext>): String {
        val str = context.reader.readWord() ?: error("期望一个权限id，但是什么都没有得到。")

        if (str.lowercase() == "@s") {
            return context.source.target.primaryPermissionId
        }

        //todo: implement @c: channel, @g: group/organization, @[id]/mention: someone

        return str
    }

    override fun buildHelp(name: String) = "[$name:permRef]"
}

fun InsertableCommandNode<AnyExecuteContext>.collectPermissionId(name: String) =
    collect(name, PermissionIdArgumentCollector())