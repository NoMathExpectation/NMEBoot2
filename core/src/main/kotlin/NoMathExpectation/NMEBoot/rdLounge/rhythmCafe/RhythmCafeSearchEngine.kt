package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe

import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.DatasetteRequest
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.LevelStatus
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.bodyToLevelStatusList
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.searchV2.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object RhythmCafeSearchEngine {
    private val logger = KotlinLogging.logger {}

    private const val MAIN_URL = "https://rhythm.cafe"
    private const val DATASETTE_URL = "https://datasette.rhythm.cafe"

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
            url(MAIN_URL)
            header("x-requested-with", "DjangoBridge")
        }
    }

    private lateinit var currentRequest: CafeSearchRequest
    private lateinit var currentSearch: CafeSearchResult

    private suspend fun searchQuery(request: CafeSearchRequest): CafeSearchResult {
        return httpClient.get(request).body<CafeSearchResponse>().results
    }

    private suspend fun sendRequest(request: CafeSearchRequest) {
        currentSearch = searchQuery(request)
        currentRequest = request
    }

    suspend fun search(query: String?, itemPerPage: Int = 10, peerReview: Boolean = false): String {
        if (itemPerPage <= 0) {
            return "请输入一个正整数"
        }

        val request = CafeSearchRequest(
            q = query ?: "",
            perPage = itemPerPage,
            peerReview = if (peerReview) CafeSearchRequest.PeerReviewFilter.APPROVED else CafeSearchRequest.PeerReviewFilter.ALL,
        )
        return runCatching {
            sendRequest(request)
            toString()
        }.getOrElse {
            logger.error(it) { "Failed to fetch search results for '${request.q}'." }
            "请求失败"
        }
    }

    suspend fun pageTo(page: Int): String {
        return try {
            sendRequest(currentRequest.copy(page = page))
            toString()
        } catch (_: HttpRequestTimeoutException) {
            "请求超时"
        }
    }

    fun getLink(index: Int) = currentSearch.hits[index - 1].rdzipUrl

//    suspend fun downloadAndUpload(group: Group, index: Int) = try {
//        FileUtils.uploadFile(group, FileUtils.download(getLink2(index)))
//    } catch (e: Exception) {
//        FileUtils.uploadFile(group, FileUtils.download(getLink(index)))
//    }


    fun isSearched() = ::currentSearch.isInitialized

    fun isNotSearched() = !isSearched()

    val currentPageItemCount get() = currentSearch.hits.size

    override fun toString() = buildString {
        append("搜索结果:\n")
        append("找到${currentSearch.estimatedTotalHits}个谱面，第${currentRequest.page}页，共${(currentSearch.estimatedTotalHits - 1) / currentRequest.perPage + 1}页\n")

        (0..<currentSearch.limit).forEach { index ->
            val matchedLevel = currentSearch.hits.getOrNull(index) ?: return@buildString

            append(currentSearch.offset + index + 1)
            append(". ${matchedLevel.peerReviewStatus}\n")
            append(matchedLevel.song)
            append("\n作者: ")
            append(matchedLevel.rawAuthors)
            append("\n")
            append(matchedLevel.rdzipUrl)
            append("\n")
        }
    }

//    @JvmName("getDescriptionJavaWithContact") // why //@jvmblockingbridge doesn't work here?
//    fun getDescription(index: Int, from: Contact): MessageChain =
//        runBlocking { RhythmCafeSearchEngine.getDescription(index, from) }

    fun getDescription(index: Int) = currentSearch.hits[index - 1].toDetailedMessage()

    suspend fun getPendingLevelCount() = datasetteQuery(DatasetteRequest.ofPending()).size

    fun sendHelp() = buildString {
        append("使用//help chart获取帮助")
    }

    suspend fun datasetteQuery(request: DatasetteRequest): List<LevelStatus> {
        return httpClient.get(request) {
            url("$DATASETTE_URL/rdlevels.json")
        }.bodyToLevelStatusList()
    }

    suspend fun getDailyBlend(): LevelStatus {
        return httpClient.get("/").body<DjangoBridgeAction<CafeIndexProps>>().props.dailyBlendLevel
    }
}