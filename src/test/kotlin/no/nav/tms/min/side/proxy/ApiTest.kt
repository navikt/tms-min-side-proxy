package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.token.support.azure.exchange.AzureService
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ApiTest {

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
            "oppfolging" to "http://veilarboppfolging.test"
        )

    @ParameterizedTest
    @ValueSource(strings = ["aap", "utkast", "personalia", "meldekort", "selector", "varsel", "syk/dialogmote"])
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
                        call.respondRawJson(testContent)
                    }
                    get("/nested/destination") {
                        call.respondRawJson(testContent)
                    }
                    get("/servererror") {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

        client.authenticatedGet("/$tjenestePath/destination").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe testContent
        }
        client.authenticatedGet("/$tjenestePath/nested/destination").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe testContent
        }

        client.authenticatedGet("/$tjenestePath/doesnotexist").status shouldBe HttpStatusCode.NotFound
        client.authenticatedGet("/$tjenestePath/servererror").status shouldBe HttpStatusCode.ServiceUnavailable
    }

    @Test
    fun `oppfolging`() = testApplication {
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
                            call.respondRawJson(testContent)
                        }

                    }
                }
            }
        }

        client.authenticatedGet("/$url").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe testContent
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["eventaggregator"])
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
                        call.receive<LoginPostRequest>().ident shouldBe "12345"
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

    private fun checkJson(receiveText: String) {
        if (receiveText == "") throw AssertionError("Post kall har ikke sendt med body")
        try {
            jsonConfig().parseToJsonElement(receiveText)
        } catch (_: Exception) {
            throw AssertionError("Post kall har sendt ugyldig json:\n$receiveText ")
        }
    }

    private fun proxyClient(httpClient: HttpClient) = ProxyHttpClient(
        httpClient = httpClient,
        tokendingsService = tokendigsMock,
        azureService = azureMock
    )

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
        aapClientId = "aapclient",
        meldekortClientId = "meldekort",
        meldekortBaseUrl = baseurl["meldekort"]!!,
        sykDialogmoteBaseUrl = baseurl["syk/dialogmote"]!!,
        sykDialogmoteClientId = "sykdialogmote",
    )
}

private const val testContent = """{"testinnhold": "her testes det innhold"}"""
private val tokendigsMock = mockk<TokendingsService>().apply {
    coEvery { exchangeToken(any(), any()) } returns "<dummytoken>"
}
private val azureMock = mockk<AzureService>().apply {
    coEvery { getAccessToken(any()) } returns "<azuretoken>"
}

private class LoginPostRequest(val ident: String)
