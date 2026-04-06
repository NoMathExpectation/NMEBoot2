package NoMathExpectation.NMEBoot.rdLounge.rhythmCafe

import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.DatasetteRequest
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.LevelStatus
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.datasette.bodyToLevelStatusList
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.typesense.TypesenseRequest
import NoMathExpectation.NMEBoot.rdLounge.rhythmCafe.data.typesense.TypesenseResult
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object RhythmCafeSearchEngine {
    private val logger = KotlinLogging.logger {}

    private const val MAIN_URL = "https://api.rhythm.cafe"
    private const val DATASETTE_URL = "https://datasette.rhythm.cafe"
    private const val API_KEY = "nicolebestgirl"

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
            header("x-typesense-api-key", API_KEY)
        }
    }

    private lateinit var currentRequest: TypesenseRequest
    private lateinit var currentSearch: TypesenseResult

    private suspend fun sendRequest(request: TypesenseRequest) {
        val response = httpClient.get(request)
        require(response.status.isSuccess()) {
            logger.error { "Error during requesting '${request.q}': ${response.status.description}" }
            "请求失败"
        }

        currentRequest = request
        currentSearch = response.body()
    }

    suspend fun search(query: String?, itemPerPage: Int = 10, peerReview: Boolean = false): String {
        if (itemPerPage <= 0) {
            return "请输入一个正整数"
        }

        return try {
            sendRequest(
                TypesenseRequest(
                    q = query ?: "",
                    per_page = itemPerPage,
                    filter_by = if (peerReview) TypesenseRequest.PR else TypesenseRequest.ANY,
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

    fun getDescription(index: Int) = currentSearch.hits[index - 1].document.toDetailedMessage()

    suspend fun getPendingLevelCount() = datasetteQuery(DatasetteRequest.ofPending()).size

    fun sendHelp() = buildString {
        append("此指令已弃用，请使用//help chart")
    }

    suspend fun datasetteQuery(request: DatasetteRequest): List<LevelStatus> {
        return httpClient.get(request) {
            url("$DATASETTE_URL/rdlevels.json")
        }.bodyToLevelStatusList()
    }
}