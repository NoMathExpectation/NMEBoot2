package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe

import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.Request
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.Result
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.DatasetteRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import love.forte.simbot.message.OfflineURIImage.Companion.toOfflineImage
import love.forte.simbot.message.buildMessages
import java.net.URI

object RhythmCafeSearchEngine {
    private const val apiKey = "nicolebestgirl"
    private val httpClient = HttpClient(CIO) {
        install(Resources)
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15 * 1000
        }
        defaultRequest {
            url("https://orchardb.fly.dev")
            header("x-typesense-api-key", apiKey)
        }
    }

    private lateinit var currentRequest: Request
    private lateinit var currentSearch: Result

    private suspend fun sendRequest(request: Request) {
        val response = httpClient.get(request)
        require(response.status.isSuccess()) { "请求失败" }

        currentRequest = request
        currentSearch = response.body()
    }

    suspend fun search(query: String?, itemPerPage: Int = 10, peerReview: Boolean = false): String {
        if (itemPerPage <= 0) {
            return "请输入一个正整数"
        }

        return try {
            sendRequest(
                Request(
                    q = query ?: "",
                    per_page = itemPerPage,
                    filter_by = if (peerReview) Request.PR else Request.ANY,
                    sort_by = if (peerReview) "_text_match:desc,indexed:desc,last_updated:desc" else "_text_match:desc,last_updated:desc"
                )
            )
            toString()
        } catch (e: HttpRequestTimeoutException) {
            "请求超时"
        }
    }

    suspend fun pageTo(page: Int): String {
        return try {
            sendRequest(currentRequest.copy(page = page))
            toString()
        } catch (e: HttpRequestTimeoutException) {
            "请求超时"
        }
    }

    fun getLink(index: Int) = currentSearch.hits[index - 1].document.url

    fun getLink2(index: Int) = currentSearch.hits[index - 1].document.url2

//    suspend fun downloadAndUpload(group: Group, index: Int) = try {
//        FileUtils.uploadFile(group, FileUtils.download(getLink2(index)))
//    } catch (e: Exception) {
//        FileUtils.uploadFile(group, FileUtils.download(getLink(index)))
//    }


    fun isSearched() = ::currentSearch.isInitialized

    fun isNotSearched() = !isSearched()

    val itemPerPage get() = currentSearch.request_params.per_page

    val currentPageItemCount get() = currentSearch.hits.size

    override fun toString() = buildString {
        append("搜索结果:\n")
        append("找到${currentSearch.found}个谱面，第${currentSearch.page}页，共${(currentSearch.found - 1) / currentSearch.request_params.per_page + 1}页\n")

        for (matchedLevelIndex in currentSearch.hits.indices) {
            val matchedLevel = currentSearch.hits[matchedLevelIndex]

            append(matchedLevelIndex + 1)
            append(". ${matchedLevel.document.peerReviewed()}\n")
            append(matchedLevel.document.song)
            append("\n作者: ")
            append(matchedLevel.document.authors.joinToString())
            append("\n")
            append(matchedLevel.document.url2)
            append("\n")
        }
    }

//    @JvmName("getDescriptionJavaWithContact") // why //@jvmblockingbridge doesn't work here?
//    fun getDescription(index: Int, from: Contact): MessageChain =
//        runBlocking { RhythmCafeSearchEngine.getDescription(index, from) }

    fun getDescription(index: Int) = buildMessages {
        val level = currentSearch.hits[index - 1].document

        +URI(level.image).toOfflineImage()

        +"歌曲名: ${level.song}\n"

        +"作曲家: ${level.artist}\n"

        +"作者: ${level.authors.joinToString()}\n"

        +"难度: ${level.getDifficulty()}\n"

        if (level.seizure_warning) {
            +"癫痫警告!\n"
        }

        +"同行评审: ${level.peerReviewed()}\n"

        +"描述:\n${level.description}\n"

        +"模式: "
        if (level.single_player) {
            +"1p "
        }
        if (level.two_player) {
            +"2p "
        }
        +"\n"

        +"标签: ${level.tags.joinToString()}"
    }

    suspend fun getPendingLevelCount() = httpClient.get(
        Request(
            filter_by = Request.PENDING,
            per_page = Request.MAX_PER_PAGE,
        )
    ).body<Result>().hits.count()

    fun sendHelp() = buildString {
        append("//chart...\n")
        append("help :显示此帮助\n")
        append("search [text] [itemPerPage] [peerReview] :搜索谱面（有空格请用引号括起）\n")
        append("page <i> :将搜索结果翻到第i页\n")
        append("info <i> :显示当前页中第i个谱面的描述\n")
        append("link <i> :获取当前页中第i个谱面的链接\n")
        append("link2 <i> :获取当前页中第i个谱面的镜像链接\n")
        append("download <i> :下载当前页中第i个谱面（还没做）")
        append("pending :获取待审谱面数量\n")
        append("subscribe <name> :订阅pr通知\n")
        append("unsubscribe :取消订阅pr通知\n")
    }

    suspend fun datasetteQuery(request: DatasetteRequest): HttpResponse {
        return httpClient.get(request)
    }
}