package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.impl.PermissionAware
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.*
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.RhythmCafeSearchEngine
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.Request
import io.github.oshai.kotlinlogging.KotlinLogging
import love.forte.simbot.ability.ReplySupport
import love.forte.simbot.ability.SendSupport

private val logger = KotlinLogging.logger { }

suspend fun <T> LiteralSelectionCommandNode<T>.commandChart()
        where T : SendSupport,
              T : ReplySupport,
              T : PermissionAware =
    literal("chart", "rdlevel")
        .requiresPermission("command.rd.fanmade.chart")
        .select {
            literals {
                literal("help", "h")
                    .executes {
                        it.send(RhythmCafeSearchEngine.sendHelp())
                    }

                literal("search", "s")
                    .optionallyCollectString("keyword")
                    .optionallyCollectInt("itemPerPage")
                    .checkInRangeNull(1, Request.MAX_PER_PAGE)
                    .optionallyCollectBoolean("peerReview")
                    .executes {
                        val keyword = getString("keyword")
                        val itemPerPage = getInt("itemPerPage") ?: 10
                        val peerReview = getBoolean("peerReview") == true

                        runCatching {
                            it.send(RhythmCafeSearchEngine.search(keyword, itemPerPage, peerReview))
                        }.onFailure { ex ->
                            logger.error(ex) { "请求失败" }
                            it.reply("请求失败")
                        }
                    }

                literal("page", "p")
                    .collectInt("page")
                    .executes {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.send("请先进行一次搜索。")
                            return@executes
                        }

                        val page = getInt("page") ?: 1
                        if (page < 1) {
                            it.send("页码不能小于1。")
                            return@executes
                        }

                        runCatching {
                            it.send(RhythmCafeSearchEngine.pageTo(page))
                        }.onFailure { ex ->
                            logger.error(ex) { "请求失败" }
                            it.reply("请求失败")
                        }
                    }

                literal("info", "i")
                    .collectInt("index")
                    .executes {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.send("请先进行一次搜索。")
                            return@executes
                        }

                        val index = getInt("index") ?: 1
                        if (index !in 1..RhythmCafeSearchEngine.itemPerPage) {
                            it.send("索引超出范围。")
                            return@executes
                        }

                        it.send(RhythmCafeSearchEngine.getDescription(index))
                    }

                literal("link", "l")
                    .collectInt("index")
                    .executes {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.send("请先进行一次搜索。")
                            return@executes
                        }

                        val index = getInt("index") ?: 1
                        if (index !in 1..RhythmCafeSearchEngine.itemPerPage) {
                            it.send("索引超出范围。")
                            return@executes
                        }

                        it.send(RhythmCafeSearchEngine.getLink(index))
                    }

                literal("link2", "l2")
                    .collectInt("index")
                    .executes {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.send("请先进行一次搜索。")
                            return@executes
                        }

                        val index = getInt("index") ?: 1
                        if (index !in 1..RhythmCafeSearchEngine.itemPerPage) {
                            it.send("索引超出范围。")
                            return@executes
                        }

                        it.send(RhythmCafeSearchEngine.getLink2(index))
                    }

                //todo: 下载谱面并上传

                literal("pending", "pd")
                    .executes {
                        kotlin.runCatching {
                            val count = RhythmCafeSearchEngine.getPendingLevelCount()
                            val countStr = if (count >= Request.MAX_PER_PAGE) "${count - 1}+" else count
                            it.send("待定谱面数：$countStr")
                        }.onFailure { e ->
                            logger.error(e) { "查询待定谱面数失败：" }
                            it.send("请求失败")
                        }
                    }
            }

            onEndOfArguments()
                .executes {
                    it.send(RhythmCafeSearchEngine.sendHelp())
                }
        }