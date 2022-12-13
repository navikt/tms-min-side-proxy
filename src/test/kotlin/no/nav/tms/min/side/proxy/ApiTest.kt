package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tms.min.side.proxy.arbeid.ArbeidConsumer
import no.nav.tms.min.side.proxy.dittnav.DittnavConsumer
import no.nav.tms.min.side.proxy.sykefravaer.SykefravaerConsumer
import no.nav.tms.min.side.proxy.utkast.UtkastConsumer
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ApiTest {
    private val proxyBasePath = "http://proxy.test"

    /*
    * Dobbelsjekk av feilhåndtering og autentisering, kan slettes etterhvert
    *
    * */
    @ParameterizedTest
    @ValueSource(strings = ["arbeid", "dittnav", "sykefravaer"])
    fun `arbeidproxy api`(originalUrl: String) = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        mockApi(
            httpClient = applicationhttpClient,
            arbeidConsumer = ArbeidConsumer(
                httpClient = applicationhttpClient, tokenFetcher = tokenfetcherMock,
                baseUrl = proxyBasePath
            ),
            dittnavConsumer = DittnavConsumer(
                httpClient = applicationhttpClient,
                tokenFetcher = tokenfetcherMock,
                baseUrl = proxyBasePath
            ),
            utkastConsumer = UtkastConsumer(
                httpClient = applicationhttpClient,
                tokenFetcher = tokenfetcherMock,
                baseUrl = proxyBasePath
            ),
            sykefraværConsumer = SykefravaerConsumer(
                httpClient = applicationhttpClient,
                tokenFetcher = tokenfetcherMock,
                baseUrl = proxyBasePath
            )
        )

        externalServices {
            hosts(proxyBasePath) {
                routing {
                    get("/destination") {
                        call.respondRawJson(testContent)
                    }
                    get("/servererror") {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }


        client.authenticatedGet("/$originalUrl/destination").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe testContent
        }

        client.get("/$originalUrl/something").assert {
            status shouldBe HttpStatusCode.Unauthorized
        }

        client.authenticatedGet("/$originalUrl/something").assert {
            status shouldBe HttpStatusCode.NotFound
        }

        client.authenticatedGet("/$originalUrl/servererror").assert {
            status shouldBe HttpStatusCode.InternalServerError
        }

    }


}

private const val testContent = """{"testinnhold": "her testes det innhold"}"""
