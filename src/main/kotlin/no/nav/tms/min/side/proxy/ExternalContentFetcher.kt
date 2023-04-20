package no.nav.tms.min.side.proxy

import io.ktor.client.statement.HttpResponse

class ExternalContentFetcher(
    private val proxyHttpClient: ProxyHttpClient,
    private val aapClientId: String,
    private val aapBaseUrl: String,
    private val meldekortClientId: String,
    private val meldekortBaseUrl: String,
    private val sykDialogmoteBaseUrl: String,
    private val sykDialogmoteClientId: String,
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
}