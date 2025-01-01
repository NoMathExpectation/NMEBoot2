package NoMathExpectation.NMEBoot.command.impl.command.rd

import NoMathExpectation.NMEBoot.command.impl.AnyExecuteContext
import NoMathExpectation.NMEBoot.command.impl.requiresPermission
import NoMathExpectation.NMEBoot.command.parser.argument.*
import NoMathExpectation.NMEBoot.command.parser.node.*
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.PeerReviewNotifier
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.RhythmCafeSearchEngine
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.Request
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
                        it.send(RhythmCafeSearchEngine.sendHelp())
                    }

                literal("search", "s")
                    .optionallyCollectString("keyword")
                    .optionallyCollectInt("itemPerPage")
                    .checkInRange(1, Request.MAX_PER_PAGE)
                    .optionallyCollectBoolean("peerReview")
                    .executes("查找谱面") {
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
                    .executes("翻页") {
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
                    .executes("获取谱面详细信息") {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.send("请先进行一次搜索。")
                            return@executes
                        }

                        val index = getInt("index") ?: 1
                        if (index !in 1..RhythmCafeSearchEngine.currentPageItemCount) {
                            it.send("索引超出范围。")
                            return@executes
                        }

                        it.send(RhythmCafeSearchEngine.getDescription(index))
                    }

                literal("link", "l")
                    .collectInt("index")
                    .executes("获取下载链接") {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.send("请先进行一次搜索。")
                            return@executes
                        }

                        val index = getInt("index") ?: 1
                        if (index !in 1..RhythmCafeSearchEngine.currentPageItemCount) {
                            it.send("索引超出范围。")
                            return@executes
                        }

                        it.send(RhythmCafeSearchEngine.getLink(index))
                    }

                literal("link2", "l2")
                    .collectInt("index")
                    .executes("获取备用下载链接") {
                        if (RhythmCafeSearchEngine.isNotSearched()) {
                            it.send("请先进行一次搜索。")
                            return@executes
                        }

                        val index = getInt("index") ?: 1
                        if (index !in 1..RhythmCafeSearchEngine.currentPageItemCount) {
                            it.send("索引超出范围。")
                            return@executes
                        }

                        it.send(RhythmCafeSearchEngine.getLink2(index))
                    }

                //todo: 下载谱面并上传

                literal("pending", "pd")
                    .executes("查询待定谱面数量") {
                        runCatching {
                            val count = RhythmCafeSearchEngine.getPendingLevelCount()
                            val countStr = if (count >= Request.MAX_PER_PAGE) "${count - 1}+" else count
                            it.send("待定谱面数：$countStr")
                        }.onFailure { e ->
                            logger.error(e) { "查询待定谱面数失败：" }
                            it.send("请求失败")
                        }
                    }

                literal("subscribe", "sub")
                    .collectString("author")
                    .executes("订阅pr通知") {
                        val author = getString("author") ?: error("author is null")
                        PeerReviewNotifier.setSubscribe(it.target, author)
                        it.send("已订阅 $author 的pr通知")
                    }

                literal("unsubscribe", "unsub")
                    .executes("取消订阅pr通知") {
                        PeerReviewNotifier.removeSubscribe(it.target)
                        it.send("已取消订阅")
                    }
            }

            onEndOfArguments()
                .executes("获取帮助") {
                    it.send(RhythmCafeSearchEngine.sendHelp())
                }
        }