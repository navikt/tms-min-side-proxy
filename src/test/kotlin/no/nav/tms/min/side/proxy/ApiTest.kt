package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ApiTest {

    private val baseurl =
        mapOf(
            "arbeid" to "http://arbeid.test",
            "dittnav" to "http://dittnav.test",
            "sykefravaer" to "http://sykefravaer.test",
            "utkast" to "http://utkast.test"
        )

    @ParameterizedTest
    @ValueSource(strings = ["arbeid", "utkast", "sykefravaer", "dittnav"])
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

    @Test
    fun `proxy post`() = testApplication {
        val tjenestePath = "dittnav"
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
        tokendingsService = tokendingsMock,
        arbeidClientId = "arbeidclient",
        arbeidBaseUrl = baseurl["arbeid"]!!,
        dittnavClientId = "dittnavclient",
        dittnavBaseUrl = baseurl["dittnav"]!!,
        sykefravaerClientId = "sykefraværtclient",
        sykefravaerBaseUrl = baseurl["sykefravaer"]!!,
        utkastClientId = "utkastclient",
        utkastBaseUrl = baseurl["utkast"]!!,
        httpClient = httpClient,
    )

}


private const val testContent = """{"testinnhold": "her testes det innhold"}"""
private val tokendingsMock = mockk<TokendingsService>().apply {
    coEvery { exchangeToken(any(), any()) } returns "<dummytoken>"
}