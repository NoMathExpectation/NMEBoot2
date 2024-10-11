package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult
import NoMathExpectation.NMEBoot.command.parser.ParserDsl

@ParserDsl
fun interface CommandNode<S> {
    suspend fun execute(context: CommandContext<S>): ExecuteResult<S>

    // gets completion options, checks if arguments are valid
    suspend fun completion(context: CommandContext<S>): HelpOption? = help(context)

    // gets help options, doesn't check arguments
    suspend fun help(context: CommandContext<S>): HelpOption? = null
}

sealed interface HelpOption {
    fun unfold(): List<Pair<List<String>, String>>

    data class Options(
        val options: List<Pair<String?, HelpOption>>,
    ) : HelpOption {
        override fun unfold() = options.flatMap {
            it.second.unfold().map { that ->
                val first = it.first?.let { prefix ->
                    that.first.toMutableList().apply {
                        add(0, prefix)
                    }
                } ?: that.first
                first to that.second
            }
        }

        override fun toString() = unfold().joinToString("\n") {
            val option = it.first.joinToString(" ")
            val help = if (it.second.isBlank()) "" else " : ${it.second}"
            "$option$help"
        }
    }

    data class Help(
        val help: String = "",
        val omitted: Boolean = false,
    ) : HelpOption {
        override fun unfold() = listOf((if (omitted) listOf("...") else listOf()) to help)

        override fun toString() = unfold().joinToString("\n") {
            val option = it.first.joinToString(" ")
            val help = if (it.second.isBlank()) "" else " : ${it.second}"
            "$option$help"
        }
    }
}