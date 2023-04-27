package no.nav.tms.min.side.proxy

import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonElement

class ExternalContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val aapClientId: String,
    private val aapBaseUrl: String,
    private val meldekortClientId: String,
    private val meldekortBaseUrl: String,
    private val sykDialogmoteBaseUrl: String,
    private val sykDialogmoteClientId: String,
    private val aiaBaseUrl: String,
    private val aiaClientId: String
) {
    suspend fun getAapContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = aapClientId,
            baseUrl = aapBaseUrl,
            proxyPath = proxyPath,
        )

    suspend fun getSykDialogmoteContent(token: String, proxyPath: String?) =
        proxyHttpClient.getContent(
            userToken = token,
            proxyPath = proxyPath,
            baseUrl = sykDialogmoteBaseUrl,
            targetAppId = sykDialogmoteClientId
        )

    suspend fun getMeldekortContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = meldekortClientId,
            baseUrl = meldekortBaseUrl,
            proxyPath = proxyPath,
            header = "TokenXAuthorization",
        )

    suspend fun getAiaContent(accessToken: String, proxyPath: String?, callId: String?) =
        proxyHttpClient.getContent(
            userToken = accessToken,
            proxyPath = proxyPath,
            baseUrl = aiaBaseUrl,
            targetAppId = aiaClientId,
            extraHeaders = callId?.let { mapOf("Nav-Call-Id" to callId) }
        )

    suspend fun postAiaContent(accessToken: String, proxyPath: String?, content: JsonElement, callId: String?) =
        proxyHttpClient.postContent(
            content = content,
            proxyPath = proxyPath,
            baseUrl = aiaBaseUrl,
            accessToken = accessToken,
            targetAppId = aiaClientId,
            extraHeaders = callId?.let { mapOf("Nav-Call-Id" to callId) }
        )
}