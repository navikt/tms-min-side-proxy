package no.nav.tms.min.side.proxy.sykefravaer


import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import no.nav.tms.min.side.proxy.authentication.IdportenUser
import no.nav.tms.min.side.proxy.common.TokenFetcher
import no.nav.tms.min.side.proxy.config.get


class SykefravaerConsumer(
    private val httpClient: HttpClient,
    private val tokenFetcher: TokenFetcher,
    private val baseUrl: String,
) {

    suspend fun getContent(user: IdportenUser, proxyPath: String?): HttpResponse {
        val accessToken = tokenFetcher.getSykefravaerApiToken(user.token)
        val url = "$baseUrl/$proxyPath"

        return httpClient.get(url, accessToken)
    }

}
