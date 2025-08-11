package no.nav.tms.min.side.proxy

import io.ktor.client.statement.HttpResponse

class ExternalContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val meldekortClientId: String,
    private val meldekortBaseUrl: String,
) {
    suspend fun getMeldekortContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = meldekortClientId,
            baseUrl = meldekortBaseUrl,
            proxyPath = proxyPath,
            header = "TokenXAuthorization",
        )
}
