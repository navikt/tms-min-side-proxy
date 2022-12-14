package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.min.side.proxy.common.ContentFetcher
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

    /*
    * Dobbelsjekk av feilhåndtering og autentisering, kan slettes etterhvert
    * */
    @ParameterizedTest
    @ValueSource(strings = ["arbeid", "utkast", "sykefravaer", "dittnav"])
    fun `proxy get api`(tjenestePath: String) = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        mockApi(
            httpClient = applicationhttpClient,
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

        client.get("/$tjenestePath/something").status shouldBe HttpStatusCode.Unauthorized
        client.authenticatedGet("/$tjenestePath/doesnotexist").status shouldBe HttpStatusCode.NotFound
        client.authenticatedGet("/$tjenestePath/servererror").status shouldBe HttpStatusCode.InternalServerError

    }

    @Test
    fun `proxy til baseurl for destinasjonstjeneste`() = testApplication {

        val applicationhttpClient = testApplicationHttpClient()
        mockApi(
            httpClient = applicationhttpClient,
            contentFetcher = contentFecther(applicationhttpClient)
        )

        externalServices {
            hosts(baseurl["utkast"]!!) {
                routing {
                    get("") {
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        client.authenticatedGet("/utkast").assert {
            status shouldBe HttpStatusCode.OK
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