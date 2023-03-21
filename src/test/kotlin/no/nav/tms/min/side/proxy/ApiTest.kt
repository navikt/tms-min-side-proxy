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
            "dittnav" to "http://dittnav.test",
            "meldekort" to "http://meldekort.test",
            "utkast" to "http://utkast.test",
            "personalia" to "http://personalia.test",
            "selector" to "http://selector.test",
            "varsel" to "http://varsel.test",
            "eventaggregator" to "http://eventAggregator.test"
        )

    @ParameterizedTest
    @ValueSource(strings = ["aap", "utkast", "dittnav", "personalia", "meldekort", "selector", "varsel"])
    fun `proxy get api`(tjenestePath: String) = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        mockApi(
            contentFetcher = contentFecther(applicationhttpClient)
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
        client.authenticatedGet("/$tjenestePath/servererror").status shouldBe HttpStatusCode.InternalServerError
    }

    @ParameterizedTest
    @ValueSource(strings = ["dittnav", "eventaggregator"])
    fun `proxy post`(tjenestePath: String) = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        mockApi(contentFetcher = contentFecther(applicationhttpClient))

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
        client.authenticatedPost("/$tjenestePath/servererror").status shouldBe HttpStatusCode.InternalServerError
    }


    @Test
    fun `post statistikk`() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        var callCount = 0
        mockApi(contentFetcher = contentFecther(applicationhttpClient))

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
        mockApi(contentFetcher = contentFecther(applicationhttpClient))

        client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK
        client.get("/internal/isReady").status shouldBe HttpStatusCode.OK
        client.get("/internal/ping").status shouldBe HttpStatusCode.OK
    }

    private fun checkJson(receiveText: String) {
        if (receiveText == "") throw AssertionError("Post kall har ikke sendt med body")
        try {
            jsonConfig().parseToJsonElement(receiveText)
        } catch (_: Exception) {
            throw AssertionError("Post kall har sendt ugyldig json:\n$receiveText ")
        }
    }

    private fun contentFecther(httpClient: HttpClient): ContentFetcher = ContentFetcher(
        tokendingsService = tokendigsMock,
        azureService = azureMock,
        aapBaseUrl = baseurl["aap"]!!,
        aapClientId = "aapclient",
        dittnavClientId = "dittnavclient",
        dittnavBaseUrl = baseurl["dittnav"]!!,
        eventAggregatorClientId = "eventaggregatorclient",
        eventAggregatorBaseUrl = baseurl["eventaggregator"]!!,
        utkastClientId = "utkastclient",
        utkastBaseUrl = baseurl["utkast"]!!,
        personaliaClientId = "personalia",
        personaliaBaseUrl = baseurl["personalia"]!!,
        meldekortClientId = "meldekort",
        meldekortBaseUrl = baseurl["meldekort"]!!,
        selectorClientId = "selector",
        selectorBaseUrl = baseurl["selector"]!!,
        varselClientId = "varsel",
        varselBaseUrl = baseurl["varsel"]!!,
        httpClient = httpClient,
        statistikkApiId = "statistikk",
        statistikkBaseApiUrl = "http://statistikk.test"
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