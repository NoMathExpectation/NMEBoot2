package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.commandConfig
import NoMathExpectation.NMEBoot.command.impl.requiresGlobalSubjectId
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.collectGreedyString
import NoMathExpectation.NMEBoot.command.parser.argument.getString
import NoMathExpectation.NMEBoot.command.parser.argument.optionallyCollectGreedyString
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.util.mutableMapStorageOf
import love.forte.simbot.message.buildMessages

private val storage = mutableMapStorageOf<String, MutableList<String>>("config/eat.json") { mutableListOf() }

lateinit var executeNode: ExecuteCommandNode<AnyExecuteContext>
    private set

suspend fun LiteralSelectionCommandNode<AnyExecuteContext>.commandEat() =
    literal("eat")
        .requiresPermission("command.common.eat")
        .requiresGlobalSubjectId()
        .select {
            literals {
                literal("add")
                    .collectGreedyString("dish")
                    .executes {
                        val globalSubjectId = it.target.globalSubjectPermissionId ?: error("没有群号")
                        val dish = getString("dish") ?: error("未提供菜品")
                        storage.referenceUpdate(globalSubjectId) { dishes ->
                            dishes += dish
                        }
                        it.reply("已加入菜单：$dish")
                    }

                literal("remove", "delete")
                    .collectGreedyString("dishOrIndex")
                    .executes {
                        val globalSubjectId = it.target.globalSubjectPermissionId ?: error("没有群号")
                        val dish = getString("dish") ?: error("未提供菜品")
                        val actualDish = storage.referenceUpdate(globalSubjectId) { dishes ->
                            dish.toIntOrNull()?.let { idx ->
                                if (idx !in 1..dishes.size) {
                                    return@referenceUpdate null
                                }
                                return@referenceUpdate dishes.removeAt(idx)
                            }
                            return@referenceUpdate dishes.indexOfFirst { it.contains(dish) }
                                .takeIf { it > 0 }
                                ?.let {
                                    dishes.removeAt(it)
                                }
                        }
                        it.reply(if (actualDish != null) "已删除菜品：$actualDish" else "未找到对应菜品")
                    }

                literal("show")
                    .executes {
                        val globalSubjectId = it.target.globalSubjectPermissionId ?: error("没有群号")
                        val dishes = storage.get(globalSubjectId)
                        it.reply(
                            "当前菜单：\n${
                                dishes.withIndex().joinToString("\n") { "${it.index + 1}. ${it.value}" }
                            }"
                        )
                    }

                literal("help")
                    .executes {
                        it.reply(buildMessages {
                            val prefix = commandConfig.get().commandPrefix
                            +"${prefix}eat...\n"
                            +"help: 显示此帮助\n"
                            +"add <dish>: 添加菜品\n"
                            +"remove <index|dish>: 删除菜品\n"
                            +"show: 显示菜单\n"
                            +"[pronoun]: 帮助决定吃什么\n"
                        })
                    }
            }

            executeNode = optionallyCollectGreedyString("pronoun")
                .executes {
                    val globalSubjectId = it.target.globalSubjectPermissionId ?: error("没有群号")
                    val pronoun = getString("pronoun") ?: "我"
                    val person = when (pronoun.trim().lowercase()) {
                        "我", "俺" -> "你"
                        "我们", "俺们" -> "你们"
                        "吾" -> "您"
                        "me", "i", "we" -> "you"
                        "my", "our" -> "your"
                        "你", "您", "你们", "您们", "机器人", "高性能机器人", "bot", "robot", "nmeboot", "atri", "亚托莉", "萝卜子", "萝卜" -> {
                            it.reply("高性能机器人不需要吃饭捏\uD83D\uDE0E")
                            return@executes
                        }

                        else -> pronoun
                    }
                    val dish = storage.get(globalSubjectId).randomOrNull() ?: "西北风"
                    it.reply("我建议${person}吃$dish")
                }
        }

val shortcutRegex = "^(.*)吃什么[?？]?$".toRegex()

suspend fun InsertableCommandNode<AnyExecuteContext>.commandEatShortcut() =
    requiresPermission("command.common.eat")
        .requiresGlobalSubjectId()
        .on {
            shortcutRegex.find(reader.string)?.groupValues?.get(1)?.ifEmpty { "我" }?.let {
                set("pronoun", it)
                true
            } ?: false
        }.forward(executeNode)