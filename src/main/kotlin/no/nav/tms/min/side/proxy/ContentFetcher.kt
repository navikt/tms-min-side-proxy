package no.nav.tms.min.side.proxy

import io.ktor.client.statement.*

class ContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val selectorClientId: String,
    private val selectorBaseUrl: String
) {

    suspend fun getProfilContent(token: String, proxyPath: String?): HttpResponse =
        proxyHttpClient.getContent(
            userToken = token,
            targetAppId = selectorClientId,
            baseUrl = selectorBaseUrl,
            proxyPath = proxyPath,
            requestTimeoutAfter = 5250
        )

    fun shutDown() {
        proxyHttpClient.shutDown()
    }
}
