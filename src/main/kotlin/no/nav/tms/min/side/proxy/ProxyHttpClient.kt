package no.nav.tms.min.side.proxy

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.token.support.azure.exchange.AzureService
import no.nav.tms.token.support.tokendings.exchange.TokendingsService

class ProxyHttpClient(
    private val httpClient: HttpClient,
    private val tokendingsService: TokendingsService,
    private val azureService: AzureService
) {
    private val log = KotlinLogging.logger {  }

    suspend fun getContent(
        userToken: String,
        targetAppId: String,
        baseUrl: String,
        proxyPath: String?,
        header: String = HttpHeaders.Authorization,
        extraHeaders: Map<String, String>? = null
    ): HttpResponse {
        if(extraHeaders!=null){
            log.info { "Request med ekstraheadere ${extraHeaders.keys.joinToString(",")} sendt" }
        }
        val exchangedToken = exchangeToken(userToken, targetAppId)
        val url = proxyPath?.let { "$baseUrl/$it" } ?: baseUrl
        return httpClient.get(url, header, exchangedToken, extraHeaders).responseIfOk()
    }

    suspend fun postWithIdentInBodyWithAzure(
        ident: String,
        clientId: String,
        baseApiUrl: String,
        proxyPath: String?
    ): HttpResponse =
        withContext(Dispatchers.IO) {
            val accessToken = azureService.getAccessToken(clientId)
            httpClient.request {
                url("$baseApiUrl/$proxyPath")
                method = HttpMethod.Post
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(LoginPostBody(ident))
            }
        }

    suspend fun postContent(
        content: ByteArray,
        proxyPath: String?,
        baseUrl: String,
        accessToken: String,
        targetAppId: String,
        extraHeaders: Map<String, String>? = null
    ): HttpResponse =
        httpClient.post(
            url = "$baseUrl/$proxyPath",
            content = content,
            accessToken = exchangeToken(accessToken, targetAppId),
            extraHeaders = extraHeaders
        ).responseIfOk()

    private suspend fun exchangeToken(
        userToken: String,
        targetAppId: String
    ) = try {
        tokendingsService.exchangeToken(userToken, targetAppId)
    } catch (e: Exception) {
        throw TokendingsException(targetAppId, userToken, e)
    }

    private suspend inline fun HttpClient.get(
        url: String,
        authorizationHeader: String,
        accessToken: String,
        extraHeaders: Map<String, String>? = null
    ): HttpResponse =
        withContext(Dispatchers.IO) {
            request {
                url(url)
                method = HttpMethod.Get
                header(authorizationHeader, "Bearer $accessToken")
                extraHeaders?.forEach {
                    header(it.key, it.value)
                }
            }
        }.responseIfOk()

    private fun HttpResponse.responseIfOk() =
        if (!status.isSuccess()) {
            throw RequestExcpetion(request.url.toString(), status, request.headers["Nav-Call-Id"])
        } else {
            this
        }

    private suspend inline fun HttpClient.post(
        url: String,
        content: ByteArray,
        accessToken: String,
        extraHeaders: Map<String, String>? = null
    ): HttpResponse =
        withContext(Dispatchers.IO) {
            request {
                url(url)
                method = HttpMethod.Post
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                extraHeaders?.forEach {
                    header(it.key, it.value)
                }
                contentType(ContentType.Application.Json)
                setBody(content)
            }
        }.responseIfOk()

    fun shutDown() {
        httpClient.close()
    }
}

data class LoginPostBody(val ident: String)

class TokendingsException(targetapp: String, val accessToken: String, originalException: Exception) :
    Exception("Feil i exchange mot tokendings for $targetapp: ${originalException.message}")

class RequestExcpetion(url: String, status: HttpStatusCode, navCallId: String? = null) : Exception(
    "proxy kall feilet mot $url med status $status ${navCallId?.let { "og callid: $it" }}"
) {
    val responseCode =
        if (status == HttpStatusCode.NotFound) HttpStatusCode.NotFound else HttpStatusCode.ServiceUnavailable
}

class MissingHeaderException(message: String) : Exception(message)
