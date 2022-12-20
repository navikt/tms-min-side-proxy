package no.nav.tms.min.side.proxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import no.nav.tms.token.support.tokendings.exchange.TokendingsService

class ContentFetcher(
    private val tokendingsService: TokendingsService,
    private val arbeidClientId: String,
    private val arbeidBaseUrl: String,
    private val dittnavClientId: String,
    private val dittnavBaseUrl: String,
    private val sykefravaerClientId: String,
    private val sykefravaerBaseUrl: String,
    private val utkastClientId: String,
    private val utkastBaseUrl: String,
    private val httpClient: HttpClient
) {

    suspend fun getUtkastContent(token: String, proxyPath: String?): HttpResponse =
        getContent(userToken = token, targetAppId = utkastClientId, baseUrl = utkastBaseUrl, proxyPath = proxyPath)

    suspend fun getArbeidContent(token: String, proxyPath: String?): HttpResponse {
        return getContent(
            userToken = token,
            targetAppId = arbeidClientId,
            baseUrl = arbeidBaseUrl,
            proxyPath = proxyPath
        )
    }

    suspend fun getSykefravaerContent(token: String, proxyPath: String?): HttpResponse =
        getContent(
            userToken = token,
            targetAppId = sykefravaerClientId,
            baseUrl = sykefravaerBaseUrl,
            proxyPath = proxyPath
        )

    suspend fun getDittNavContent(token: String, proxyPath: String?): HttpResponse =
        getContent(
            userToken = token,
            targetAppId = dittnavClientId,
            baseUrl = dittnavBaseUrl,
            proxyPath = proxyPath
        )

    suspend fun postDittNavContent(token: String, content: JsonElement, proxyPath: String?): HttpResponse {
        val exchangedToken = tokendingsService.exchangeToken(token, targetApp = dittnavClientId)
        return httpClient.post("$dittnavBaseUrl/$proxyPath", content, exchangedToken)
    }

    private suspend fun getContent(
        userToken: String,
        targetAppId: String,
        baseUrl: String,
        proxyPath: String?
    ): HttpResponse {
        val exchangedToken = tokendingsService.exchangeToken(userToken, targetAppId)
        val url = proxyPath?.let { "$baseUrl/$it" } ?: baseUrl
        return httpClient.get(url, exchangedToken)
    }

    fun shutDown() {
        httpClient.close()
    }

}

suspend inline fun <reified T> HttpClient.get(url: String, accessToken: String): T = withContext(Dispatchers.IO) {
    request {
        url(url)
        method = HttpMethod.Get
        header(HttpHeaders.Authorization, "Bearer $accessToken")
    }.body()
}

suspend inline fun <reified T> HttpClient.post(url: String, content: JsonElement, accessToken: String): T =
    withContext(Dispatchers.IO) {
        request {
            url(url)
            method = HttpMethod.Post
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(content)
        }
    }.body()