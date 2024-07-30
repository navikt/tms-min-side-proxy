package no.nav.tms.min.side.proxy

import io.ktor.client.statement.HttpResponse

class ExternalContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val meldekortClientId: String,
    private val meldekortBaseUrl: String,
    private val aiaBaseUrl: String,
    private val aiaClientId: String,
) {
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
}
