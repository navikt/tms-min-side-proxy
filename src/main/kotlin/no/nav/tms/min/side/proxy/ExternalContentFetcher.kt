package no.nav.tms.min.side.proxy

import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonElement

class ExternalContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val meldekort: Pair<String, String>,
    private val aap: Pair<String, String>,
    private val sykDialogmote: Pair<String, String>,
    private val aia: Pair<String, String>
) {
    suspend fun getAapContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = aap.clientId,
            baseUrl = aap.baseUrl,
            proxyPath = proxyPath,
        )

    suspend fun getSykDialogmoteContent(token: String, proxyPath: String?) =
        proxyHttpClient.getContent(
            userToken = token,
            proxyPath = proxyPath,
            baseUrl = sykDialogmote.baseUrl,
            targetAppId = sykDialogmote.clientId
        )

    suspend fun getMeldekortContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = meldekort.clientId,
            baseUrl = meldekort.baseUrl,
            proxyPath = proxyPath,
            header = "TokenXAuthorization",
        )

    suspend fun getAiaContent(accessToken: String, proxyPath: String?, callId: String?) =
        proxyHttpClient.getContent(
            userToken = accessToken,
            proxyPath = proxyPath,
            baseUrl = aia.baseUrl,
            targetAppId = aia.clientId,
            extraHeaders = callId?.let { mapOf("Nav-Call-Id" to callId) }
        )

    suspend fun postAiaContent(
        accessToken: String,
        proxyPath: String?,
        content: JsonElement,
        callId: String?,
    ) =
        proxyHttpClient.postContent(
            content = content,
            proxyPath = proxyPath,
            baseUrl = aia.baseUrl,
            accessToken = accessToken,
            targetAppId = aia.clientId,
            extraHeaders = callId?.let { mapOf("Nav-Call-Id" to callId) }
        )
}