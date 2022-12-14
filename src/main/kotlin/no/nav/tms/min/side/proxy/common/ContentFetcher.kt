package no.nav.tms.min.side.proxy.common

import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonElement
import no.nav.tms.min.side.proxy.config.get
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

    fun postDittNavContent(accessToken: String, content: JsonElement, proxyPath: String?): HttpResponse {
        TODO("Not yet implemented")
    }

    private suspend fun getContent(
        userToken: String,
        targetAppId: String,
        baseUrl: String,
        proxyPath: String?
    ): HttpResponse {
        val exchangedToken = tokendingsService.exchangeToken(userToken, targetAppId)
        val url = proxyPath?.let { "$baseUrl/$it" } ?: utkastBaseUrl
        return httpClient.get(url, exchangedToken)
    }

}