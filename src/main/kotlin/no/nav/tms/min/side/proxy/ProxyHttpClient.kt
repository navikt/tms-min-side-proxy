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
import io.ktor.client.plugins.*
import no.nav.tms.token.support.entraid.token.fetcher.EntraIdTokenFetcher
import no.nav.tms.token.support.user.token.exchange.UserTokenExchanger

class ProxyHttpClient(
    private val httpClient: HttpClient,
    private val userTokenFetcher: UserTokenExchanger,
    private val entraIdTokenFetcher: EntraIdTokenFetcher
) {
    private val log = KotlinLogging.logger {  }

    suspend fun getContent(
        userToken: String,
        targetAppId: String,
        baseUrl: String,
        proxyPath: String?,
        header: String = HttpHeaders.Authorization,
        extraHeaders: Map<String, String>? = null,
        requestTimeoutAfter: Long = 3000
    ): HttpResponse {
        if(extraHeaders!=null){
            log.info { "Request med ekstraheadere ${extraHeaders.keys.joinToString(",")} sendt" }
        }
        val exchangedToken = exchangeToken(userToken, targetAppId)
        val url = proxyPath?.let { "$baseUrl/$it" } ?: baseUrl
        return httpClient.get(url, header, exchangedToken, extraHeaders, requestTimeoutAfter).responseIfOk()
    }

    private suspend fun exchangeToken(
        userToken: String,
        targetApp: String
    ) = try {
        userTokenFetcher.exchangeToken(userToken, targetApp)
    } catch (e: Exception) {
        throw TokendingsException(targetApp, e)
    }

    private suspend inline fun HttpClient.get(
        url: String,
        authorizationHeader: String,
        accessToken: String,
        extraHeaders: Map<String, String>? = null,
        requestTimeoutAfter: Long,
    ): HttpResponse =
        withContext(Dispatchers.IO) {
            request {
                url(url)
                method = HttpMethod.Get
                header(authorizationHeader, "Bearer $accessToken")
                extraHeaders?.forEach {
                    header(it.key, it.value)
                }
                timeout {
                    requestTimeoutMillis = requestTimeoutAfter
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

class TokendingsException(targetapp: String, originalException: Exception) :
    RuntimeException("Feil i exchange mot tokendings for $targetapp", originalException)

class RequestExcpetion(url: String, status: HttpStatusCode, navCallId: String? = null) : Exception(
    "proxy kall feilet mot $url med status $status ${navCallId?.let { "og callid: $it" }}"
) {
    val responseCode =
        if (status == HttpStatusCode.NotFound) HttpStatusCode.NotFound else HttpStatusCode.ServiceUnavailable
}

class MissingHeaderException(message: String) : Exception(message)
