package no.nav.tms.min.side.proxy

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import mu.KotlinLogging
import no.nav.tms.token.support.azure.exchange.AzureService
import no.nav.tms.token.support.tokendings.exchange.TokendingsService

class ContentFetcher(
    private val tokendingsService: TokendingsService,
    private val azureService: AzureService,
    private val aapClientId: String,
    private val aapBaseUrl: String,
    private val dittnavClientId: String,
    private val dittnavBaseUrl: String,
    private val eventAggregatorClientId: String,
    private val eventAggregatorBaseUrl: String,
    private val meldekortClientId: String,
    private val meldekortBaseUrl: String,
    private val utkastClientId: String,
    private val utkastBaseUrl: String,
    private val personaliaClientId: String,
    private val personaliaBaseUrl: String,
    private val selectorClientId: String,
    private val selectorBaseUrl: String,
    private val varselClientId: String,
    private val varselBaseUrl: String,
    private val statistikkApiId: String,
    private val statistikkBaseApiUrl: String,
    private val httpClient: HttpClient
) {

    private val secureLog = KotlinLogging.logger("secureLog")
    private val log = KotlinLogging.logger {}

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
        return withResponseLogging { httpClient.post("$dittnavBaseUrl/$proxyPath", content, exchangedToken) }
    }

    suspend fun postEventAggregatorContent(token: String, content: JsonElement, proxyPath: String?): HttpResponse {
        val exchangedToken = tokendingsService.exchangeToken(token, targetApp = eventAggregatorClientId)
        return withResponseLogging {
            httpClient.post(
                "$eventAggregatorBaseUrl/$proxyPath",
                content,
                exchangedToken
            )
        }
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

    suspend fun getVarselContent(token: String, proxyPath: String?): HttpResponse =
        getContent(
            userToken = token,
            targetAppId = varselClientId,
            baseUrl = varselBaseUrl,
            proxyPath = proxyPath,
        )

    suspend fun postInnloggingStatistikk(ident: String): HttpResponse = withContext(Dispatchers.IO) {
        val accessToken = azureService.getAccessToken(statistikkApiId)

        withResponseLogging {
            httpClient.request {
                url("$statistikkBaseApiUrl/innlogging")
                method = HttpMethod.Post
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(LoginPostBody(ident))
            }
        }
    }


    private suspend fun getContent(
        userToken: String,
        targetAppId: String,
        baseUrl: String,
        proxyPath: String?,
        header: String = HttpHeaders.Authorization
    ): HttpResponse {
        val exchangedToken = tokendingsService.exchangeToken(userToken, targetAppId)
        val url = proxyPath?.let { "$baseUrl/$it" } ?: baseUrl
        return withResponseLogging {
            httpClient.get(url, header, exchangedToken)
        }
    }

    fun shutDown() {
        httpClient.close()
    }

    private suspend fun withResponseLogging(
        function: suspend () -> HttpResponse
    ): HttpResponse =
        function().also { response ->
            if (!response.status.isSuccess()) {
                val body = response.body<String>()
                val url = response.request.url
                log.warn { "Request til $url feiler med ${response.status}" }
                secureLog.warn {
                    "proxy kall feilet mot $url.\nFeilkode: ${response.status} \nInnhold: $body\npayload fra request: $response.request.content"
                }
            }
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

@Serializable
data class LoginPostBody(val ident: String)