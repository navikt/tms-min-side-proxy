package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GetRoutesTest {

    private val baseurl =
        mapOf(
            "aap" to "http://aap.test",
            "meldekort" to "http://meldekort.test",
            "utkast" to "http://utkast.test",
            "personalia" to "http://personalia.test",
            "selector" to "http://selector.test",
            "varsel" to "http://varsel.test",
            "eventaggregator" to "http://eventAggregator.test",
            "syk/dialogmote" to "http://isdialog.test",
            "oppfolging" to "http://veilarboppfolging.test",
            "aia" to "http://paw.test"
        )

    @ParameterizedTest
    @ValueSource(strings = ["aap", "utkast", "personalia", "meldekort", "selector", "varsel", "syk/dialogmote", "aia"])
    fun `proxy get api`(tjenestePath: String) = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)

        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        externalServices {
            hosts(baseurl[tjenestePath]!!) {
                routing {
                    get("/destination") {
                        call.respondRawJson(defaultTestContent)
                    }
                    get("/nested/destination") {
                        call.respondRawJson(defaultTestContent)
                    }
                    get("/servererror") {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

        client.authenticatedGet("/$tjenestePath/destination").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe defaultTestContent
        }
        client.authenticatedGet("/$tjenestePath/nested/destination").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe defaultTestContent
        }

        client.authenticatedGet("/$tjenestePath/doesnotexist").status shouldBe HttpStatusCode.NotFound
        client.authenticatedGet("/$tjenestePath/servererror").status shouldBe HttpStatusCode.ServiceUnavailable
    }

    @Test
    fun oppfolging() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        val url = "oppfolging"

        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        externalServices {
            hosts(baseurl["oppfolging"]!!) {
                routing {
                    get("/api/niva3/underoppfolging") {
                        val navconsumerHeader = call.request.header("Nav-Consumer-Id")
                        if (navconsumerHeader == null) {
                            call.respond(HttpStatusCode.BadRequest)
                        } else {
                            navconsumerHeader shouldBe "min-side:tms-min-side-proxy"
                            call.respondRawJson(defaultTestContent)
                        }

                    }
                }
            }
        }

        client.authenticatedGet("/$url").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe defaultTestContent
        }
    }

    @Test
    fun healtApiTest() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK
        client.get("/internal/isReady").status shouldBe HttpStatusCode.OK
        client.get("/internal/ping").status shouldBe HttpStatusCode.OK
    }

    @Test
    fun authPing() = testApplication {
        mockApi(contentFetcher = mockk(), externalContentFetcher = mockk())
        client.get("/authPing").status shouldBe HttpStatusCode.OK
    }
    private fun contentFecther(proxyHttpClient: ProxyHttpClient): ContentFetcher = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        eventAggregatorClientId = "eventaggregatorclient",
        eventAggregatorBaseUrl = baseurl["eventaggregator"]!!,
        utkastClientId = "utkastclient",
        utkastBaseUrl = baseurl["utkast"]!!,
        personaliaClientId = "personalia",
        personaliaBaseUrl = baseurl["personalia"]!!,
        selectorClientId = "selector",
        selectorBaseUrl = baseurl["selector"]!!,
        varselClientId = "varsel",
        varselBaseUrl = baseurl["varsel"]!!,
        statistikkClientId = "statistikk",
        statistikkBaseApiUrl = "http://statistikk.test",
        oppfolgingBaseUrl = baseurl["oppfolging"]!!,
        oppfolgingClientId = "veilarboppfolging"
    )

    private fun externalContentFetcher(proxyHttpClient: ProxyHttpClient) = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        aapBaseUrl = baseurl["aap"]!!,
        aapClientId = "aap",
        meldekortClientId = "meldekort",
        meldekortBaseUrl = baseurl["meldekort"]!!,
        sykDialogmoteBaseUrl = baseurl["syk/dialogmote"]!!,
        sykDialogmoteClientId = "sykdialogmote",
        aiaBaseUrl = baseurl["aia"]!!,
        aiaClientId = "aia",
    )
}
