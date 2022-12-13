package no.nav.tms.min.side.proxy.utkast

import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import no.nav.tms.min.side.proxy.common.TokenFetcher
import no.nav.tms.min.side.proxy.config.get


class UtkastConsumer(
    private val httpClient: HttpClient,
    private val tokenFetcher: TokenFetcher,
    private val baseUrl: String,
) {

    suspend fun getContent(token: String, proxyPath: String?): HttpResponse {
        val accessToken = tokenFetcher.getUtkastApiToken(token)
        val url = "$baseUrl/$proxyPath"

        return httpClient.get(url, accessToken)
    }

}
