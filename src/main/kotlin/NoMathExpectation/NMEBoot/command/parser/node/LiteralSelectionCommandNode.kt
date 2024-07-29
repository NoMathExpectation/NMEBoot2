package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

class LiteralSelectionCommandNode<S>(
    val options: MutableMap<String, CommandNode<S>> = mutableMapOf(),
) : CommandNode<S> {
    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        val subCommand = context.reader.readWord() ?: return ExecuteResult(
            context.source,
            0,
            1,
            parseExceptions = listOf(IllegalArgumentException("缺失子指令。"))
        )
        val node = subCommand.let { options[it] } ?: return ExecuteResult(
            context.source,
            0,
            1,
            parseExceptions = listOf(IllegalArgumentException("未知的子指令 $subCommand。"))
        )
        return node.execute(context)
    }

    operator fun set(vararg names: String, node: CommandNode<S>) = names.forEach {
        options[it] = node
    }
}

fun <S> LiteralSelectionCommandNode<S>.literal(vararg names: String): ForwardCommandNode<S> {
    val node = ForwardCommandNode<S>()
    set(*names, node = node)
    return node
}

fun <S> InsertableCommandNode<S>.literals(init: LiteralSelectionCommandNode<S>.() -> Unit): LiteralSelectionCommandNode<S> {
    val node = LiteralSelectionCommandNode<S>()
    node.init()
    insert(node)
    return node
}