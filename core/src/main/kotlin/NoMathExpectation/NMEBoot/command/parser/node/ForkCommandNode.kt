package NoMathExpectation.NMEBoot.command.parser.node

import NoMathExpectation.NMEBoot.command.parser.CommandContext
import NoMathExpectation.NMEBoot.command.parser.CommandException
import NoMathExpectation.NMEBoot.command.parser.ExecuteResult

typealias TransformClause<S> = suspend CommandContext<S>.(S) -> List<CommandContext<S>>

fun interface SourceTransformClause<S> {
    suspend fun CommandContext<S>.transform(source: S): List<S>
}

class ForkCommandNode<S>(
    override var next: CommandNode<S> = commandNodeTodo(),
    private val transform: TransformClause<S>
) : SingleNextCommandNode<S> {
    constructor(next: CommandNode<S> = commandNodeTodo(), sourceTransform: SourceTransformClause<S>) : this(
        next,
        mappingWrapper(sourceTransform)
    )

    companion object {
        private fun <S> mappingWrapper(sourceTransform: SourceTransformClause<S>): TransformClause<S> =
            { source -> with(sourceTransform) { transform(source) }.map { copy(it) } }
    }

    override suspend fun execute(context: CommandContext<S>): ExecuteResult<S> {
        val forks = transform(context, context.source)
        var accepted = 0
        val exceptions = mutableListOf<CommandException>()

        forks.forEach {
            val forkResult = next.execute(it)
            accepted += forkResult.accepted
            exceptions += forkResult.exceptions
        }

        return ExecuteResult(
            context.source,
            accepted,
            forks.size,
            exceptions = exceptions,
        )
    }

    override suspend fun completion(context: CommandContext<S>) =
        transform(context, context.source)
            .firstOrNull()
            ?.let {
                next.completion(it)
            }
}

fun <S> InsertableCommandNode<S>.fork(transform: TransformClause<S>): ForkCommandNode<S> =
    ForkCommandNode(transform = transform).also { insert(it) }

fun <S> InsertableCommandNode<S>.forkWithSource(sourceTransform: SourceTransformClause<S>): ForkCommandNode<S> =
    ForkCommandNode(sourceTransform = sourceTransform).also { insert(it) }