package no.nav.tms.min.side.proxy

import io.ktor.client.statement.*
import kotlinx.serialization.json.JsonElement
import no.nav.tms.token.support.tokendings.exchange.TokenXHeader

class ContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val utkastClientId: String,
    private val utkastBaseUrl: String,
    private val personaliaClientId: String,
    private val personaliaBaseUrl: String,
    private val selectorClientId: String,
    private val selectorBaseUrl: String,
    private val statistikkClientId: String,
    private val statistikkBaseApiUrl: String,
    private val oppfolgingClientId: String,
    private val oppfolgingBaseUrl: String
) {
    suspend fun getUtkastContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = utkastClientId,
            baseUrl = utkastBaseUrl,
            proxyPath = proxyPath
        )


    suspend fun getPersonaliaContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = personaliaClientId,
            baseUrl = personaliaBaseUrl,
            proxyPath = proxyPath,
        )

    suspend fun getProfilContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = selectorClientId,
            baseUrl = selectorBaseUrl,
            proxyPath = proxyPath,
        )


    suspend fun postInnloggingStatistikk(ident: String): HttpResponse = proxyHttpClient.postWithIdentInBodyWithAzure(
        ident = ident,
        baseApiUrl = statistikkBaseApiUrl,
        proxyPath = "/innlogging",
        clientId = statistikkClientId,
    )

    fun shutDown() {
        proxyHttpClient.shutDown()
    }

    suspend fun getOppfolgingContent(token: String, proxypath: String) = proxyHttpClient.getContent(
        userToken = token,
        targetAppId = oppfolgingClientId,
        baseUrl = oppfolgingBaseUrl,
        proxyPath = proxypath,
        extraHeaders = mapOf("Nav-Consumer-Id" to "min-side:tms-min-side-proxy")
    )
}
