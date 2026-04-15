package NoMathExpectation.NMEBoot.command.impl.command.rd

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.*
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.PeerReviewNotifier
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.RhythmCafeSearchEngine
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.DatasetteRequest
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.searchV2.CafeSearchRequest
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger { }

suspend fun <T> LiteralSelectionCommandNode<T>.commandChart()
        where T : AnyExecuteContext =
    literal("chart", "rdlevel")
        .requiresPermission("command.rd.fanmade.chart")
        .select {
            help = "查询cafe谱面"

            literals {
                blockOptions = false

                literal("help", "h")
                    .executes("获取帮助") {
                        it.reply(RhythmCafeSearchEngine.sendHelp())
                    }

                literal("random", "r")
                    .optionallyCollectBoolean("peerReview")
                    .executes("随机谱面") {
                        val peerReview = getBoolean("peerReview") == true

                        runCatching {
                            val query = DatasetteRequest.ofRandom(1, peerReview)
                            val level = RhythmCafeSearchEngine.datasetteQuery(query)
                                .firstOrNull() ?: run {
                                it.reply("没有可以随机的谱面...")
                                return@executes
                            }

                            it.reply(level.toDetailedMessage())
                        }.onFailure { ex ->
                            logger.error(ex) { "随机谱面失败" }
                            it.reply("请求失败")
                        }
                    }

                literal("search", "s")
                    .optionallyCollectString("keyword")
                    .optionallyCollectInt("itemPerPage")
                    .checkInRange(1, CafeSearchRequest.MAX_PER_PAGE)
                    .optionallyCollectBoolean("peerReview")
                    .executes("查找谱面") {
                        val keyword = getString("keyword")
                        val itemPerPage = getInt("itemPerPage") ?: 10
                        val peerReview = getBoolean("peerReview") == true

                        runCatching {
                            it.reply(RhythmCafeSearchEngine.search(keyword, itemPerPage, peerReview))
                        }.onFailure { ex ->
                            logger.error(ex) { "请求失败" }
                            it.reply("请求失败")
                        }
                    }

                literal("page", "p")
                    .collectInt("page")
                    .executes("翻页") {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.reply("请先进行一次搜索。")
                            return@executes
                        }

                        val page = getInt("page") ?: 1
                        if (page < 1) {
                            it.reply("页码不能小于1。")
                            return@executes
                        }

                        runCatching {
                            it.reply(RhythmCafeSearchEngine.pageTo(page))
                        }.onFailure { ex ->
                            logger.error(ex) { "请求失败" }
                            it.reply("请求失败")
                        }
                    }

                literal("info", "i")
                    .collectInt("index")
                    .executes("获取谱面详细信息") {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.reply("请先进行一次搜索。")
                            return@executes
                        }

                        val index = getInt("index") ?: 1
                        if (index !in 1..RhythmCafeSearchEngine.currentPageItemCount) {
                            it.reply("索引超出范围。")
                            return@executes
                        }

                        it.send(RhythmCafeSearchEngine.getDescription(index))
                    }

                literal("link", "l")
                    .collectInt("index")
                    .executes("获取下载链接") {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.reply("请先进行一次搜索。")
                            return@executes
                        }

                        val index = getInt("index") ?: 1
                        if (index !in 1..RhythmCafeSearchEngine.currentPageItemCount) {
                            it.reply("索引超出范围。")
                            return@executes
                        }

                        it.reply(RhythmCafeSearchEngine.getLink(index))
                    }

                //todo: 下载谱面并上传

                literal("pending", "pd")
                    .executes("查询待定谱面数量") {
                        runCatching {
                            val count = RhythmCafeSearchEngine.getPendingLevelCount()
                            val countStr = if (count >= CafeSearchRequest.MAX_PER_PAGE) "${count - 1}+" else count
                            it.reply("待定谱面数：$countStr")
                        }.onFailure { e ->
                            logger.error(e) { "查询待定谱面数失败：" }
                            it.reply("请求失败")
                        }
                    }

                literal("subscribe", "sub")
                    .optionallyCollectString("author")
                    .executes("订阅pr通知") {
                        val author = getString("author") ?: run {
                            val name = PeerReviewNotifier.getSubscribeName(it.target)
                            if (name != null) {
                                it.reply("你当前订阅了 $name 的pr通知")
                            } else {
                                it.reply("你当前没有订阅pr通知")
                            }
                            return@executes
                        }
                        PeerReviewNotifier.setSubscribe(it.target, author)
                        it.reply("已订阅 $author 的pr通知")
                    }

                literal("unsubscribe", "unsub")
                    .executes("取消订阅pr通知") {
                        PeerReviewNotifier.removeSubscribe(it.target)
                        it.reply("已取消订阅")
                    }

                literal("daily", "d")
                    .executes("获取每日推荐谱面") {
                        val level = RhythmCafeSearchEngine.getDailyBlend() ?: run {
                            it.reply("今日暂无推荐谱面")
                            return@executes
                        }
                        it.send(level.toDetailedMessage())
                    }
            }

            onEndOfArguments()
                .executes("获取帮助") {
                    it.reply(RhythmCafeSearchEngine.sendHelp())
                }
        }