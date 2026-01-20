package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresGlobalSubjectId
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.source.requiresBotModerator
import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.collectString
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.argument.optionallyCollectGreedyString
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.message.deserializeToMessage
import NoMathExpectation.NMEBoot.message.toSerialized
import NoMathExpectation.NMEBoot.util.MutableMapStorage
import NoMathExpectation.NMEBoot.util.mutableMapStorageOf
import kotlinx.serialization.Serializable
import love.forte.simbot.definition.Actor
import love.forte.simbot.message.Message

object TagData {
    @Serializable
    data class Tag(
        var content: String = "",
        var description: String? = null,
    )

    @Serializable
    data class GroupedTagData(
        val tags: MutableMap<String, Tag> = mutableMapOf()
    )

    private val storage: MutableMapStorage<String, GroupedTagData> =
        mutableMapStorageOf("data/tag.json") { GroupedTagData() }

    suspend fun <R> modify(id: String, block: suspend GroupedTagData.() -> R): R {
        return storage.referenceUpdate(id, block)
    }

    suspend fun queryTag(globalSubject: String, tagName: String, context: Actor?): Message? {
        return modify(globalSubject) {
            val content = tags[tagName]
                ?: tags.asSequence().firstOrNull { (k, _) -> k.equals(tagName, true) }?.value
                ?: return@modify null
            return@modify content.content.deserializeToMessage(context) { persistent = true }
        }
    }

    suspend fun updateTag(globalSubject: String, tagName: String, message: Message, context: Actor?) {
        modify(globalSubject) {
            val tag = tags.getOrPut(tagName) { Tag() }
            tag.content = message.toSerialized(context) { persistent = true }
        }
    }

    suspend fun updateTagDescription(globalSubject: String, tagName: String, description: String?): Boolean {
        return modify(globalSubject) {
            val tag = tags[tagName] ?: return@modify false
            tag.description = description
            true
        }
    }

    suspend fun deleteTag(globalSubject: String, tagName: String): Boolean {
        return modify(globalSubject) {
            return@modify tags.remove(tagName) != null
        }
    }

    suspend fun listTags(globalSubject: String, keyword: String?): List<Pair<String, Tag>> {
        return modify(globalSubject) {
            if (keyword == null) {
                return@modify tags.toList()
            }

            return@modify tags.filter { (k, _) ->
                k.contains(keyword, true)
            }.toList()
        }
    }
}

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandTag() =
    literal("tag", "tags", "t")
        .requiresPermission("command.common.tag")
        .requiresGlobalSubjectId()
        .select {
            blockOptions = true
            help = "获取标签帮助"

            collectString("name")
                .select {
                    blockOptions = false
                    help = "获取标签帮助"

                    onEndOfArguments()
                        .executes("获取标签帮助") {
                            val globalSubjectId =
                                it.target.globalSubjectPermissionId ?: error("missing global subject id")
                            val globalSubject = it.target.globalSubject
                            val tagName = getString("name") ?: error("missing tag name")

                            val tagContent = TagData.queryTag(globalSubjectId, tagName, globalSubject)
                            if (tagContent != null) {
                                it.reply(tagContent)
                                return@executes
                            }

                            val tagList = TagData.listTags(globalSubjectId, tagName)
                            if (tagList.isEmpty()) {
                                it.reply("找不到标签 $tagName。")
                            }

                            it.reply(
                                tagList.joinToString("\n") { (name, tag) ->
                                    if (tag.description != null) {
                                        "$name: ${tag.description}"
                                    } else {
                                        name
                                    }
                                }
                            )
                        }

                    requiresBotModerator()
                        .literals {
                            blockOptions = false
                            help = "设置标签"

                            literal("update")
                                .optionallyCollectGreedyString("content")
                                .executes("更新标签内容") {
                                    val globalSubjectId =
                                        it.target.globalSubjectPermissionId ?: error("missing global subject id")
                                    val globalSubject = it.target.globalSubject
                                    val tagName = getString("name") ?: error("missing tag name")
                                    val tagContent = getString("content")
                                        ?.deserializeToMessage { formatLineFeeds = false }
                                        ?: it.originalMessage?.referenceMessage()?.messages
                                        ?: run {
                                            it.reply("缺少字符串内容或引用")
                                            return@executes
                                        }

                                    TagData.updateTag(globalSubjectId, tagName, tagContent, globalSubject)

                                    it.reply("标签 $tagName 已更新。")
                                }

                            literal("description", "desc")
                                .collectGreedyString("description")
                                .executes("更新标签描述") {
                                    val globalSubjectId =
                                        it.target.globalSubjectPermissionId ?: error("missing global subject id")
                                    val tagName = getString("name") ?: error("missing tag name")
                                    val description = getString("description")

                                    val success = TagData.updateTagDescription(globalSubjectId, tagName, description)
                                    if (!success) {
                                        it.reply("标签 $tagName 不存在。")
                                    }

                                    it.reply("标签 $tagName 的描述已更新。")
                                }

                            literal("delete", "del", "remove", "rm")
                                .executes("删除标签") {
                                    val globalSubjectId =
                                        it.target.globalSubjectPermissionId ?: error("missing global subject id")
                                    val tagName = getString("name") ?: error("missing tag name")

                                    val success = TagData.deleteTag(globalSubjectId, tagName)
                                    if (!success) {
                                        it.reply("标签 $tagName 不存在。")
                                    }

                                    it.reply("标签 $tagName 已删除。")
                                }
                        }
                }

            executes("获取可用的标签") {
                val globalSubjectId = it.target.globalSubjectPermissionId ?: error("missing global subject id")
                val tags = TagData.listTags(globalSubjectId, null)
                if (tags.isEmpty()) {
                    it.reply("没有可用的标签。")
                    return@executes
                }

                it.reply(
                    tags.joinToString("\n") { (name, tag) ->
                        if (tag.description != null) {
                            "$name: ${tag.description}"
                        } else {
                            name
                        }
                    }
                )
            }
        }