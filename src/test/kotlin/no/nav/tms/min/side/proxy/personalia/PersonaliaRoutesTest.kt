package no.nav.tms.min.side.proxy.personalia

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.common.testutils.initExternalServices
import no.nav.tms.min.side.proxy.HttpRouteProvider
import no.nav.tms.min.side.proxy.jsonConfig
import no.nav.tms.min.side.proxy.mockApi
import no.nav.tms.min.side.proxy.respondRawJson
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class PersonaliaRoutesTest {

    private val pdlClientId = "pdl"
    private val pdlApiUrl = "http://pdl/graphql"
    private val pdlBehandlingsnummer = "B000"

    private val token = "<token>"

    private val tokendingsService: TokendingsService = mockk()

    @AfterEach
    fun cleanup() {
        clearMocks(tokendingsService)
    }

    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `henter helnavn fra PDL`() = testApplication {
        val fornavn = "Navn1"
        val mellomnavn = "Navn2"
        val etternavn = "Navn3"
        val requestAssertion = PdlRequestAssertion(fornavn, mellomnavn, etternavn)

        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        initExternalServices(
            "http://pdl",
            HttpRouteProvider("/graphql", routeMethodFunction = Routing::post, assert = requestAssertion::assertion)
        )

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            personaliaFetcher = personaliaFetcher()
        )

        client.get("/personalia").let {
            it.status shouldBe HttpStatusCode.OK

            objectMapper.readTree(it.bodyAsText())["navn"]
                .asText() shouldBe "$fornavn $mellomnavn $etternavn"
        }
    }

    @Test
    fun `Ignorerer manglende mellomnavn`() = testApplication {
        val fornavn = "Navn1"
        val etternavn = "Navn3"
        val requestAssertion = PdlRequestAssertion(fornavn, null, etternavn)

        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        initExternalServices(
            "http://pdl",
            HttpRouteProvider("/graphql", routeMethodFunction = Routing::post, assert = requestAssertion::assertion)
        )
        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            personaliaFetcher = personaliaFetcher()
        )

        client.get("/personalia").let {
            it.status shouldBe HttpStatusCode.OK

            objectMapper.readTree(it.bodyAsText())["navn"]
                .asText() shouldBe "$fornavn $etternavn"
        }
    }

    @Test
    fun `Svarer med partial content hvis pdl-response ikke har data`() = testApplication {
        val requestAssertion = PdlRequestAssertion(null, null, null, hasEmptyResponse = true)

        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        initExternalServices(
            "http://pdl",
            HttpRouteProvider("/graphql", routeMethodFunction = Routing::post, assert = requestAssertion::assertion)
        )

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            personaliaFetcher = personaliaFetcher()
        )

        client.get("/personalia").let {
            it.status shouldBe HttpStatusCode.PartialContent
        }
    }

    @Test
    fun `Svarer med brukers ident`() = testApplication {
        val requestAssertion = PdlRequestAssertion(null, null, null, hasEmptyResponse = true)

        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        initExternalServices(
            "http://pdl",
            HttpRouteProvider("/graphql", routeMethodFunction = Routing::post, assert = requestAssertion::assertion)
        )

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            personaliaFetcher = personaliaFetcher()
        )

        client.get("/personalia").let {
            it.status shouldBe HttpStatusCode.PartialContent

            objectMapper.readTree(it.bodyAsText())["ident"]
                .asText() shouldBe "12345"
        }
    }

    @Test
    fun `Svarer med navn fra pdl og ident`() = testApplication {
        val fornavn = "Navn1"
        val mellomnavn = "Navn2"
        val etternavn = "Navn3"
        val requestAssertion = PdlRequestAssertion(fornavn, mellomnavn, etternavn, false)


        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        initExternalServices(
            "http://pdl",
            HttpRouteProvider("/graphql", routeMethodFunction = Routing::post, assert = requestAssertion::assertion)
        )

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            personaliaFetcher = personaliaFetcher()
        )

        client.get("/personalia").let {
            it.status shouldBe HttpStatusCode.OK

            objectMapper.readTree(it.bodyAsText()).run {
                get("navn").asText() shouldBe "$fornavn $mellomnavn $etternavn"
                get("ident").asText() shouldBe "12345"
            }
        }
    }

    @Test
    fun `Svarer kun med ident hvis pdl feiler`() = testApplication {
        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        initExternalServices(
            "http://pdl",
            HttpRouteProvider("/graphql", routeMethodFunction = Routing::post, assert = {
                it.respond(HttpStatusCode.InternalServerError)
            })
        )

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            personaliaFetcher = personaliaFetcher()
        )

        client.get("/personalia").let {
            it.status shouldBe HttpStatusCode.PartialContent

            objectMapper.readTree(it.bodyAsText()).run {
                get("navn").isNull shouldBe true
                get("ident").asText() shouldBe "12345"
            }
        }
    }

    private fun ApplicationTestBuilder.personaliaFetcher() = PersonaliaFetcher(
        client = createClient {
            install(ContentNegotiation) {
                jackson {
                    jsonConfig()
                }
            }
        },
        pdlUrl = pdlApiUrl,
        pdlClientId = pdlClientId,
        pdlBehandlingsnummer = pdlBehandlingsnummer,
        tokendingsService = tokendingsService
    )

    private fun validResponse(fornavn: String, mellomnavn: String?, etternavn: String) = """
       { 
           "data": {
               "hentPerson": {
                   "navn": [ { 
                       "fornavn": "$fornavn", 
                       "mellomnavn": ${mellomnavn.jsonNode()}, 
                       "etternavn": "$etternavn"
                   } ]
               }
           },
           "errors": null,
           "extensions": null
       }
    """

    private fun responseWithError() = """
        { 
           "data": null,
           "errors": [
                {
                    "message": "Feil"
                }
           ],
           "extensions": null
       }
    """

    private fun responseWithoutData() = """
        {
           "data": null,
           "errors": null,
           "extensions": null
        }
    """.trimIndent()

    private fun String?.jsonNode() = when (this) {
        null -> "null"
        else -> "\"$this\""
    }

    inner class PdlRequestAssertion(
        private val fornavn: String?,
        private val mellomnavn: String?,
        private val etternavn: String?,
        private val hasEmptyResponse: Boolean = false
    ) {

        suspend fun assertion(call: ApplicationCall) {
            call.request.header(HttpHeaders.Authorization) shouldContain "token"
            call.request.header("Behandlingsnummer") shouldBe pdlBehandlingsnummer

            if (hasEmptyResponse) {
                call.respondRawJson(responseWithoutData())
            } else if (fornavn == null || etternavn == null)
                call.respondRawJson(responseWithError())
            else
                call.respondRawJson(validResponse(fornavn, mellomnavn, etternavn))
        }
    }
}
