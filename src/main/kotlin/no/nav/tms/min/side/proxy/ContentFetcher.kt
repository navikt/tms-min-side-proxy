package no.nav.tms.min.side.proxy

import io.ktor.client.statement.*
import kotlinx.serialization.json.JsonElement

class ContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val eventAggregator:ApplicationVariables,
    private val utkast: ApplicationVariables,
    private val personalia: ApplicationVariables,
    private val selector: ApplicationVariables,
    private val varsel: ApplicationVariables,
    private val statistikk : ApplicationVariables,
    private val oppfolgingClientId: String,
    private val oppfolgingBaseUrl: String
) {
    suspend fun getUtkastContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = utkast.clientId,
            baseUrl = utkast.baseUrl,
            proxyPath = proxyPath
        )

    suspend fun postEventAggregatorContent(token: String, content: JsonElement, proxyPath: String?): HttpResponse =
        proxyHttpClient.postContent(
            content = content,
            proxyPath = proxyPath,
            baseUrl = eventAggregator.baseUrl,
            accessToken = token,
            targetAppId = eventAggregator.clientId,
        )


    suspend fun getPersonaliaContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = personalia.clientId,
            baseUrl = personalia.baseUrl,
            proxyPath = proxyPath,
        )

    suspend fun getProfilContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = selector.clientId,
            baseUrl = selector.baseUrl,
            proxyPath = proxyPath,
        )

    suspend fun getVarselContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = varsel.clientId,
            baseUrl = varsel.baseUrl,
            proxyPath = proxyPath,
        )

    suspend fun postInnloggingStatistikk(ident: String): HttpResponse = proxyHttpClient.postWithIdentInBodyWithAzure(
        ident = ident,
        baseApiUrl = statistikk.baseUrl,
        proxyPath = "/innlogging",
        clientId = statistikk.clientId,
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