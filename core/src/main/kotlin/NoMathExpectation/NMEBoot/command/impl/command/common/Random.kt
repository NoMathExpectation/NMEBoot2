package NoMathExpectation.NMEBoot.command.impl.command.common

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.*
import NoMathExpectation.NMEBoot.command.parser.get
import NoMathExpectation.NMEBoot.command.parser.getOrPut
import NoMathExpectation.NMEBoot.command.parser.node.*
import love.forte.simbot.ability.ReplySupport

suspend fun <S> LiteralSelectionCommandNode<S>.commandRandom()
        where S : PermissionAware, S : ReplySupport =
    literal("random")
        .requiresPermission("command.common.random")
        .select {
            blockOptions = false
            help = "生成随机数或随机一个选项"

            collectLong("min")
                .collectLong("max")
                .executes("随机最小值到最大值之间的整数") {
                    val min = getLong("min") ?: error("未提供最小值")
                    val max = getLong("max") ?: error("未提供最大值")
                    if (min > max) {
                        it.reply("最小值不能大于最大值")
                        return@executes
                    }
                    val randomValue = (min..max).random()
                    it.reply(randomValue.toString())
                }

            collectLong("max")
                .executes("随机1到最大值之间的整数") {
                    val max = getLong("max") ?: error("未提供最大值")
                    if (max <= 0) {
                        it.reply("最大值必须大于0")
                        return@executes
                    }
                    val randomValue = (1..max).random()
                    it.reply(randomValue.toString())
                }

            literals {
                blockOptions = false

                val uniformCollect = literal("uniform")
                    .collectString("item")
                uniformCollect.executes {
                    val list = getOrPut<MutableList<String>>("list") { mutableListOf() }
                    list += getString("item") ?: error("未提供物品")
                }.select {
                    help = "随机一个选项"

                    forward(uniformCollect)

                    executes("随机一个选项") {
                        val list = get<MutableList<String>>("list") ?: error("没有选项")
                        if (list.isEmpty()) {
                            it.reply("没有可选项")
                            return@executes
                        }
                        it.reply(list.random())
                    }
                }

                val weightedCollect = literal("weighted")
                    .collectString("item")
                weightedCollect.collectLong("weight")
                    .checkInRange(min = 1)
                    .executes {
                        val item = getString("item") ?: error("未提供物品")
                        val weight = getLong("weight") ?: error("未提供权重")
                        check(weight > 0) { "权重必须大于0" }
                        val list = getOrPut<MutableList<Pair<String, Long>>>("list") { mutableListOf() }
                        list += item to weight
                    }.select {
                        help = "带权重随机一个选项"

                        forward(weightedCollect)

                        executes("带权重随机一个选项") {
                            val list = get<MutableList<Pair<String, Long>>>("list") ?: error("没有选项")
                            if (list.isEmpty()) {
                                it.reply("没有可选项")
                                return@executes
                            }
                            val totalWeight = list.sumOf { it.second }
                            val randomValue = (1..totalWeight).random()
                            var cumulativeWeight = 0L
                            for ((item, weight) in list) {
                                cumulativeWeight += weight
                                if (randomValue <= cumulativeWeight) {
                                    it.reply(item)
                                    return@executes
                                }
                            }
                        }
                    }
            }
        }