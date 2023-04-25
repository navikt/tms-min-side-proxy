package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PostRoutesTest {

    private val baseurl =
        mapOf(
            "eventaggregator" to "http://eventAggregator.test",
            "aia" to "http://paw.test"
        )


    @ParameterizedTest
    @ValueSource(strings = ["eventaggregator","aia"])
    fun `proxy post`(tjenestePath: String) = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)

        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        externalServices {
            hosts(baseurl[tjenestePath]!!) {
                routing {
                    post("/destination") {
                        checkJson(call.receiveText())
                        call.respond(HttpStatusCode.OK)
                    }
                    post("/nested/destination") {
                        checkJson(call.receiveText())
                        call.respond(HttpStatusCode.OK)
                    }
                    post("/servererror") {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

        client.authenticatedPost("/$tjenestePath/destination").assert {
            status shouldBe HttpStatusCode.OK
        }
        client.authenticatedPost("/$tjenestePath/nested/destination").assert {
            status shouldBe HttpStatusCode.OK
        }

        client.authenticatedPost("/$tjenestePath/doesnotexist").status shouldBe HttpStatusCode.NotFound
        client.authenticatedPost("/$tjenestePath/servererror").status shouldBe HttpStatusCode.ServiceUnavailable
    }


    @Test
    fun `post statistikk`() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        var callCount = 0
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        externalServices {
            hosts("http://statistikk.test") {
                routing {
                    post("/innlogging") {
                        callCount += 1
                        call.receive<StatistikkPostRequest>().ident shouldBe "12345"
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        client.authenticatedPost("/statistikk/innlogging").assert {
            status shouldBe HttpStatusCode.OK
        }
        callCount shouldBe 1
    }

    private fun externalContentFetcher(proxyHttpClient: ProxyHttpClient) = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        aapClientId = "aap",
        aapBaseUrl = "",
        meldekortClientId = "meldekort",
        meldekortBaseUrl = "",
        sykDialogmoteBaseUrl = "",
        sykDialogmoteClientId = "",
        aiaBaseUrl = baseurl["aia"]!!,
        aiaClientId = "aia"
    )

    private fun contentFecther(proxyHttpClient: ProxyHttpClient) = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        eventAggregatorClientId = "eventaggregator",
        eventAggregatorBaseUrl = baseurl["eventaggregator"]!!,
        utkastClientId = "",
        utkastBaseUrl = "",
        personaliaClientId = "",
        personaliaBaseUrl = "",
        selectorClientId = "",
        selectorBaseUrl = "",
        varselClientId = "",
        varselBaseUrl = "",
        statistikkClientId = "statistikk",
        statistikkBaseApiUrl = "http://statistikk.test",
        oppfolgingClientId = "",
        oppfolgingBaseUrl = ""
    )
}

private class StatistikkPostRequest(val ident: String)