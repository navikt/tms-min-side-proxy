package no.nav.tms.min.side.proxy.arbeid


import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import no.nav.tms.min.side.proxy.common.TokenFetcher
import no.nav.tms.min.side.proxy.config.get
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUser

class ArbeidConsumer(
    private val httpClient: HttpClient,
    private val tokenFetcher: TokenFetcher,
    private val baseUrl: String,
) {

    suspend fun getContent(user: IdportenUser, proxyPath: String?): HttpResponse {
        val accessToken = tokenFetcher.getArbeidApiToken(user.tokenString)
        val url = "$baseUrl/$proxyPath"

        return httpClient.get(url, accessToken)
    }

}
