package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandParseException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

class LiteralSelectionCommandNode<S>(
    val options: MutableMap<String, CommandNode<S>> = mutableMapOf(),
) : CommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        val subCommand = context.reader.readWord() ?: return ExecuteResult(
            context.source,
            0,
            1,
            exceptions = listOf(CommandParseException("缺失子指令。"))
        )
        val node = subCommand.let { options[it] } ?: return ExecuteResult(
            context.source,
            0,
            1,
            exceptions = listOf(CommandParseException("未知的子指令 $subCommand。"))
        )
        return node.execute(context)
    }

    operator fun set(vararg names: String, node: CommandNode<S>) = names.forEach {
        options[it] = node
    }
}

fun <S> LiteralSelectionCommandNode<S>.literal(vararg names: String): ForwardCommandNode<S> {
    check(names.isNotEmpty()) { "Literal names not provided." }
    val node = ForwardCommandNode<S>()
    set(*names, node = node)
    return node
}

inline fun <S> InsertableCommandNode<S>.literals(init: LiteralSelectionCommandNode<S>.() -> Unit): LiteralSelectionCommandNode<S> {
    val node = LiteralSelectionCommandNode<S>()
    node.init()
    insert(node)
    return node
}