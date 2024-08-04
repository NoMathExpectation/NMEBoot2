package NoMathExpectation.NMEBoot.message

import NoMathExpectation.NMEBoot.command.source.CommandSource
import love.forte.simbot.message.Message

fun interface MessageProcessor {
    suspend fun CommandSource<*>.process(msg: Message): Message

    companion object {
        private val processors = mutableListOf<MessageProcessor>()

        fun registerProcessor(processor: MessageProcessor) {
            processors.add(processor)
        }

        operator fun plusAssign(processor: MessageProcessor) = registerProcessor(processor)

        suspend fun CommandSource<*>.processMessage(msg: Message) = processors.fold(msg) { m, p ->
            with(p) {
                process(m)
            }
        }
    }
}