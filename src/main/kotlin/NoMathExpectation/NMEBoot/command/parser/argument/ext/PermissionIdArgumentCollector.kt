package NoMathExpectation.NMEBoot.command.parser.argument.ext

import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.impl.source.uidToPermissionId
import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.argument.ArgumentCollector
import NoMathExpectation.NMEBoot.command.parser.node.InsertableCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.collect

class PermissionIdArgumentCollector : ArgumentCollector<CommandSource<*>, String> {
    override suspend fun collect(context: CommandContext<CommandSource<*>>): String {
        val str = context.reader.readWord() ?: error("期望一个权限id，但是什么都没有得到。")

        if (str.lowercase() == "@s") {
            return context.source.uidToPermissionId
        }

        //todo: implement @c: channel, @g: group/organization, @[id]/mention: someone

        return str
    }
}

fun InsertableCommandNode<CommandSource<*>>.collectPermissionId(name: String) =
    collect(name, PermissionIdArgumentCollector())