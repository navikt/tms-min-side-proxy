package no.nav.tms.min.side.proxy

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import no.nav.tms.token.support.azure.exchange.AzureService
import no.nav.tms.token.support.tokendings.exchange.TokendingsService

class ContentFetcher(
    private val tokendingsService: TokendingsService,
    private val azureService: AzureService,
    private val aapClientId: String,
    private val aapBaseUrl: String,
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
    private val sykDialogmoteBaseUrl: String,
    private val sykDialogmoteClientId: String,
    private val httpClient: HttpClient
) {

    suspend fun getSykDialogmoteContent(token: String, proxyPath: String?) =
        getContent(
            userToken = token,
            proxyPath = proxyPath,
            baseUrl = sykDialogmoteBaseUrl,
            targetAppId = sykDialogmoteClientId
        )

    suspend fun getUtkastContent(token: String, proxyPath: String?): HttpResponse =
        getContent(userToken = token, targetAppId = utkastClientId, baseUrl = utkastBaseUrl, proxyPath = proxyPath)

    suspend fun postEventAggregatorContent(token: String, content: JsonElement, proxyPath: String?): HttpResponse =
        httpClient.post(
            "$eventAggregatorBaseUrl/$proxyPath",
            content,
            exchangeToken(token, eventAggregatorClientId)
        )


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
        httpClient.request {
            url("$statistikkBaseApiUrl/innlogging")
            method = HttpMethod.Post
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(LoginPostBody(ident))
        }
    }


    private suspend fun getContent(
        userToken: String,
        targetAppId: String,
        baseUrl: String,
        proxyPath: String?,
        header: String = HttpHeaders.Authorization
    ): HttpResponse {
        val exchangedToken = exchangeToken(userToken, targetAppId)
        val url = proxyPath?.let { "$baseUrl/$it" } ?: baseUrl
        return httpClient.get(url, header, exchangedToken).responseIfOk()
    }

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
        accessToken: String
    ): HttpResponse =
        withContext(Dispatchers.IO) {
            request {
                url(url)
                method = HttpMethod.Get
                header(authorizationHeader, "Bearer $accessToken")
            }
        }.responseIfOk()

    private fun HttpResponse.responseIfOk() =
        if (!status.isSuccess()) {
            throw RequestExcpetion(request.url.toString(), status)
        } else {
            this
        }

    private suspend inline fun HttpClient.post(url: String, content: JsonElement, accessToken: String): HttpResponse =
        withContext(Dispatchers.IO) {
            request {
                url(url)
                method = HttpMethod.Post
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(content)
            }
        }.responseIfOk()

    fun shutDown() {
        httpClient.close()
    }
}


@Serializable
data class LoginPostBody(val ident: String)

class TokendingsException(targetapp: String, val accessToken: String, originalException: Exception) :
    Exception("Feil i exchange mot tokendings for $targetapp: ${originalException.message}")

class RequestExcpetion(url: String, status: HttpStatusCode) : Exception(
    "proxy kall feilet mot $url med status $status "
) {
    val responseCode =
        if (status == HttpStatusCode.NotFound) HttpStatusCode.NotFound else HttpStatusCode.ServiceUnavailable
}