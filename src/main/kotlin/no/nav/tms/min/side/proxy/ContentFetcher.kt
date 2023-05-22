package no.nav.tms.min.side.proxy

import io.ktor.client.statement.*
import kotlinx.serialization.json.JsonElement

private const val SIDECAR_WORKAROUND_HEADER = "PROXY_TOKEN"
class ContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val eventAggregatorClientId: String,
    private val eventAggregatorBaseUrl: String,
    private val utkastClientId: String,
    private val utkastBaseUrl: String,
    private val personaliaClientId: String,
    private val personaliaBaseUrl: String,
    private val selectorClientId: String,
    private val selectorBaseUrl: String,
    private val varselClientId: String,
    private val varselBaseUrl: String,
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

    suspend fun postEventAggregatorContent(token: String, content: JsonElement, proxyPath: String?): HttpResponse =
        proxyHttpClient.postContent(
            content = content,
            proxyPath = proxyPath,
            baseUrl = eventAggregatorBaseUrl,
            accessToken = token,
            targetAppId = eventAggregatorClientId,
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

    suspend fun getVarselContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = varselClientId,
            baseUrl = varselBaseUrl,
            proxyPath = proxyPath,
            extraHeaders = mapOf(SIDECAR_WORKAROUND_HEADER to token)
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
