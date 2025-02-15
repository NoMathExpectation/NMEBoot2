package NoMathExpectation.NMEBoot.util

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.File
import java.io.InputStream

object TransferSh {
    private val httpClient = HttpClient {
        install(Resources)

        defaultRequest {
            url("https://transfer.sh")
        }
    }

    data class UploadOptions(
        var maxDay: Int? = null,
        var maxDownloads: Int? = null,
    )

    suspend fun upload(
        fileName: String,
        inputStream: InputStream,
        optionsBlock: UploadOptions.() -> Unit = {}
    ): String {
        val options = UploadOptions().apply(optionsBlock)
        val response = httpClient.put(fileName) {
            setBody(inputStream.toByteReadChannel())

            options.maxDay?.let {
                header("Max-Days", it)
            }
            options.maxDownloads?.let {
                header("Max-Downloads", it)
            }
        }
        return response.bodyAsText()
    }

    suspend fun upload(file: File, optionsBlock: UploadOptions.() -> Unit = {}) =
        upload(file.name, file.inputStream(), optionsBlock)
}