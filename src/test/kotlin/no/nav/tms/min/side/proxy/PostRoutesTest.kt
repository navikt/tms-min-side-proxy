package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.tms.common.testutils.initExternalServices
import org.junit.jupiter.api.Test

class PostRoutesTest {


    @Test
    fun `post statistikk`() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        var callCount = 0
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient),
            navnFetcher = mockk(),
            personaliaFetcher = mockk()
        )

        initExternalServices(
            "http://statistikk.test",
            HttpRouteProvider("innlogging", routeMethodFunction = Routing::post, assert = { call ->
                callCount += 1
                call.receive<StatistikkPostRequest>().ident shouldBe "12345"
                call.respond(HttpStatusCode.OK)
            })
        )

        client.authenticatedPost("/statistikk/innlogging").assert {
            status shouldBe HttpStatusCode.OK
        }
        callCount shouldBe 1
    }

    private fun externalContentFetcher(proxyHttpClient: ProxyHttpClient) = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        meldekortClientId = "meldekort",
        meldekortBaseUrl = "",
        aiaBaseUrl = "placeholder",
        aiaClientId = "aia"
    )

    private fun contentFecther(proxyHttpClient: ProxyHttpClient) = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        utkastClientId = "",
        utkastBaseUrl = "",
        selectorClientId = "",
        selectorBaseUrl = "",
        statistikkClientId = "statistikk",
        statistikkBaseApiUrl = "http://statistikk.test",
        oppfolgingClientId = "",
        oppfolgingBaseUrl = ""
    )
}

private class StatistikkPostRequest(val ident: String)
