package no.nav.tms.min.side.proxy.arbeid;

import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tms.min.side.proxy.assert
import no.nav.tms.min.side.proxy.testApplicationHttpClient
import no.nav.tms.min.side.proxy.mockApi
import no.nav.tms.min.side.proxy.respondRawJson
import no.nav.tms.min.side.proxy.tokenfetcherMock
import org.junit.jupiter.api.Test

class ArbeidApiTest {
    private val arbeidapiUrl = "http://arbeid.test"

    @Test
    fun testGetArbeid() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        mockApi(
            httpClient = applicationhttpClient,
            arbeidConsumer = ArbeidConsumer(
                httpClient = applicationhttpClient, tokenFetcher = tokenfetcherMock,
                baseUrl = arbeidapiUrl
            )
        )

        externalServices {
            hosts(arbeidapiUrl) {
                routing {
                    get("/veientil") {
                        call.respondRawJson(testContent)
                    }
                }
            }
        }
        client.get("/arbeid/veientil").assert {
            status shouldBe HttpStatusCode.OK
            this.headers["contentType"] shouldBe "application/json"
            bodyAsText() shouldBe testContent
        }
    }
}

private const val testContent = """{"testinnhold": "her testes det innhold"}"""