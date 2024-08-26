package NoMathExpectation.NMEBoot.command.impl.command

import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.impl.source.CommandSource
import NoMathExpectation.NMEBoot.command.impl.source.send
import NoMathExpectation.NMEBoot.command.parser.argument.*
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.RhythmCafeSearchEngine
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.Request
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun LiteralSelectionCommandNode<CommandSource<*>>.commandChart() =
    literal("chart", "rdlevel")
        .requiresPermission("command.rd.fanmade.chart")
        .select {
            literals {
                literal("help", "h")
                    .executes {
                        it.send(RhythmCafeSearchEngine.sendHelp())
                    }

                literal("search", "s")
                    .collectString("keyword")
                    .optionallyCollectInt("itemPerPage")
                    .optionallyCollectBoolean("peerReview")
                    .executes {
                        val keyword = getString("keyword")
                        val itemPerPage = getInt("itemPerPage") ?: 10
                        val peerReview = getBoolean("peerReview") ?: false
                        it.send(RhythmCafeSearchEngine.search(keyword, itemPerPage, peerReview))
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

                        it.send(RhythmCafeSearchEngine.pageTo(page))
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
                    .requiresPermission("command.rd")
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

            executes {
                it.send(RhythmCafeSearchEngine.sendHelp())
            }
        }