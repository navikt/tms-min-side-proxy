package no.nav.tms.min.side.proxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.client.request.*
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import no.nav.tms.token.support.tokendings.exchange.TokendingsService

class ContentFetcher(
    private val tokendingsService: TokendingsService,
    private val aapClientId: String,
    private val aapBaseUrl: String,
    private val dittnavClientId: String,
    private val dittnavBaseUrl: String,
    private val meldekortClientId: String,
    private val meldekortBaseUrl: String,
    private val utkastClientId: String,
    private val utkastBaseUrl: String,
    private val personaliaClientId: String,
    private val personaliaBaseUrl: String,
    private val selectorClientId: String,
    private val selectorBaseUrl: String,
    private val httpClient: HttpClient
) {

    suspend fun getUtkastContent(token: String, proxyPath: String?): HttpResponse =
        getContent(userToken = token, targetAppId = utkastClientId, baseUrl = utkastBaseUrl, proxyPath = proxyPath)

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

    suspend fun getPersonaliaContent(token: String, proxyPath: String?): HttpResponse =
        getContent(
            userToken = token,
            targetAppId = personaliaClientId,
            baseUrl = personaliaBaseUrl,
            proxyPath = proxyPath,
        )

    suspend fun getMeldekortContent(token: String, proxyPath: String?): HttpResponse =
        getContent(
            userToken = token,
            targetAppId = meldekortClientId,
            baseUrl = meldekortBaseUrl,
            proxyPath = proxyPath,
            header = "TokenXAuthorization",
        )

    suspend fun getAapContent(token: String, proxyPath: String?): HttpResponse =
        getContent(
            userToken = token,
            targetAppId = aapClientId,
            baseUrl = aapBaseUrl,
            proxyPath = proxyPath,
        )

    suspend fun getProfilContent(token: String, proxyPath: String?): HttpResponse =
        getContent(
            userToken = token,
            targetAppId = selectorClientId,
            baseUrl = selectorBaseUrl,
            proxyPath = proxyPath,
        )

    private suspend fun getContent(
        userToken: String,
        targetAppId: String,
        baseUrl: String,
        proxyPath: String?,
        header: String = HttpHeaders.Authorization
    ): HttpResponse {
        val exchangedToken = tokendingsService.exchangeToken(userToken, targetAppId)
        val url = proxyPath?.let { "$baseUrl/$it" } ?: baseUrl
        return httpClient.get(url, header, exchangedToken)
    }

    fun shutDown() {
        httpClient.close()
    }

}

suspend inline fun <reified T> HttpClient.get(url: String, authorizationHeader: String, accessToken: String): T =
    withContext(Dispatchers.IO) {
        request {
            url(url)
            method = HttpMethod.Get
            header(authorizationHeader, "Bearer $accessToken")
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
