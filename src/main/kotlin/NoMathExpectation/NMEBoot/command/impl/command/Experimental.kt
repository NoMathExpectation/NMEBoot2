package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.collectLong
import NoMathExpectation.NMEBoot.command.parser.argument.getLong
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.node.LiteralSelectionCommandNode
import NoMathExpectation.NMEBoot.command.parser.node.executes
import NoMathExpectation.NMEBoot.command.parser.node.literal
import NoMathExpectation.NMEBoot.command.parser.node.literals
import NoMathExpectation.NMEBoot.util.TransferSh
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import love.forte.simbot.annotations.ExperimentalSimbotAPI
import love.forte.simbot.common.id.LongID.Companion.ID
import love.forte.simbot.component.kook.KookMember
import love.forte.simbot.component.onebot.v11.core.actor.OneBotMember
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.outputStream

private val logger = KotlinLogging.logger { }

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandTransfer() =
    literal("transfer")
        .requiresPermission("command.experimental.transfer")
        .collectGreedyString("text")
        .executes {
            val str = getString("text") ?: " "
            val inputStream = str.byteInputStream()
            val uuid = UUID.randomUUID().toString()
            val link = TransferSh.upload(
                uuid,
                inputStream,
            ) {
                maxDay = 1
            }

            it.reply(link)
        }

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandRef() =
    literal("ref")
        .requiresPermission("command.experimental.ref")
        .executes {
            logger.info { }
        }

@Serializable
data class GroupMemberInfo(
    val id: String,
    val name: String,
    val nick: String? = null,
    val roles: List<String> = listOf(),
)

private val outputJson = Json {
    prettyPrint = true
}

@OptIn(ExperimentalSimbotAPI::class, ExperimentalSerializationApi::class)
suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandExport() =
    literal("export")
        .requiresPermission("command.experimental.export")
        .literals {
            literal("group")
                .collectLong("group")
                .executes {
                    val relation = it.bot?.groupRelation ?: run {
                        it.reply("此平台不支持获取群")
                        return@executes
                    }

                    val id = getLong("group") ?: error("无法获取群号")
                    val group = relation.group(id.ID) ?: run {
                        it.reply("无法获取群信息")
                        return@executes
                    }

                    Path("data", "export").toFile().mkdirs()
                    val file = Path("data", "export", "group-$id.json")
                    val list = buildList {
                        group.members.asFlow().collect { member ->
                            val roles = when (member) {
                                is OneBotMember -> member.role?.name?.let { roleName -> listOf(roleName) } ?: listOf()
                                is KookMember -> member.roles.asFlow().map { role -> role.name }.toList()
                                else -> listOf()
                            }

                            add(GroupMemberInfo(member.id.toString(), member.name, member.nick, roles))
                        }
                    }

                    file.outputStream(StandardOpenOption.CREATE).use { stream ->
                        outputJson.encodeToStream(list, stream)
                    }

                    it.reply("已导出")
                }
        }