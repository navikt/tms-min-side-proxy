package no.nav.tms.min.side.proxy.dittnav

import io.ktor.client.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.JsonElement
import no.nav.tms.min.side.proxy.authentication.IdportenUser
import no.nav.tms.min.side.proxy.common.TokenFetcher
import no.nav.tms.min.side.proxy.config.get
import no.nav.tms.min.side.proxy.config.post


class DittnavConsumer(
    private val httpClient: HttpClient,
    private val tokenFetcher: TokenFetcher,
    private val baseUrl: String,
) {

    suspend fun getContent(user: IdportenUser, proxyPath: String?): HttpResponse {
        val accessToken = tokenFetcher.getDittnavApiToken(user.token)
        val url = "$baseUrl/$proxyPath"

        return httpClient.get(url, accessToken)
    }

    suspend fun postContent(user: IdportenUser, content: JsonElement, proxyPath: String?): HttpResponse {
        val accessToken = tokenFetcher.getDittnavApiToken(user.token)
        val url = "$baseUrl/$proxyPath"

        return httpClient.post(url, content, accessToken)
    }

}
