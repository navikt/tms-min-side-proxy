package no.nav.tms.min.side.proxy.personalia

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.min.side.proxy.jsonConfig
import no.nav.tms.min.side.proxy.mockApi
import no.nav.tms.min.side.proxy.respondRawJson
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class NavnRoutesTest {

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
        val pdlResponse = PdlResponseConfig(fornavn, mellomnavn, etternavn)

        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        setupPdlAsExternal(pdlResponse)

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            navnFetcher = navnFetcher(),
            personaliaFetcher = mockk()
        )

        client.get("/personalia/navn").let {
            it.status shouldBe HttpStatusCode.OK

            objectMapper.readTree(it.bodyAsText())["navn"]
                .asText() shouldBe "$fornavn $mellomnavn $etternavn"
        }
    }

    @Test
    fun `Ignorerer manglende mellomnavn`() = testApplication {
        val fornavn = "Navn1"
        val etternavn = "Navn3"
        val pdlResponse = PdlResponseConfig(fornavn, null, etternavn)


        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        setupPdlAsExternal(pdlResponse)

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            navnFetcher = navnFetcher(),
            personaliaFetcher = mockk()
        )

        client.get("/personalia/navn").let {
            it.status shouldBe HttpStatusCode.OK

            objectMapper.readTree(it.bodyAsText())["navn"]
                .asText() shouldBe "$fornavn $etternavn"
        }
    }

    @Test
    fun `Svarer med feil hvis pdl-response har feil`() = testApplication {
        val pdlResponse = PdlResponseConfig(null, null, null)

        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        setupPdlAsExternal(pdlResponse)

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            navnFetcher = navnFetcher(),
            personaliaFetcher = mockk()
        )

        client.get("/personalia/navn").let {
            it.status shouldBe HttpStatusCode.InternalServerError
        }
    }

    @Test
    fun `Svarer med feil hvis pdl-response ikke har data`() = testApplication {
        val pdlResponse = PdlResponseConfig(null, null, null, hasEmptyResponse = true)

        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        setupPdlAsExternal(pdlResponse)

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            navnFetcher = navnFetcher(),
            personaliaFetcher = mockk()
        )

        client.get("/personalia/navn").let {
            it.status shouldBe HttpStatusCode.InternalServerError
        }
    }

    @Test
    fun `Svarer med brukers ident`() = testApplication {
        val pdlResponse = PdlResponseConfig(null, null, null, hasEmptyResponse = true)

        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        setupPdlAsExternal(pdlResponse)

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            navnFetcher = navnFetcher(),
            personaliaFetcher = mockk()
        )

        client.get("/personalia/ident").let {
            it.status shouldBe HttpStatusCode.OK

            objectMapper.readTree(it.bodyAsText())["ident"]
                .asText() shouldBe "12345"
        }
    }

    @Test
    fun `Svarer med navn fra pdl og ident`() = testApplication {
        val fornavn = "Navn1"
        val mellomnavn = "Navn2"
        val etternavn = "Navn3"
        val pdlResponse = PdlResponseConfig(fornavn, mellomnavn, etternavn, false)


        coEvery {
            tokendingsService.exchangeToken(any(), pdlClientId)
        } returns token

        externalServices {
            hosts("http://pdl") {
                routing {

                }
            }
        }

        setupPdlAsExternal(pdlResponse)

        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            navnFetcher = navnFetcher(),
            personaliaFetcher = mockk()
        )

        client.get("/navn").let {
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


        setupPdlAsExternal(
            responseConfig = null,
            handlerOverride = {
                call.respond(HttpStatusCode.InternalServerError)
            }
        )


        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            navnFetcher = navnFetcher(),
            personaliaFetcher = mockk()
        )

        client.get("/navn").let {
            it.status shouldBe HttpStatusCode.OK

            objectMapper.readTree(it.bodyAsText()).run {
                get("navn").isNull shouldBe true
                get("ident").asText() shouldBe "12345"
            }
        }
    }

    private fun ApplicationTestBuilder.navnFetcher() = NavnFetcher(
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

    inner class PdlResponseConfig(
        val fornavn: String?,
        val mellomnavn: String?,
        val etternavn: String?,
        val hasEmptyResponse: Boolean = false
    )

    private fun TestApplicationBuilder.setupPdlAsExternal(
        responseConfig: PdlResponseConfig?,
        handlerOverride: (suspend RoutingContext.() -> Unit)? = null
    ) {
        val defaultHandler: suspend RoutingContext.(PdlResponseConfig) -> Unit = { config ->
            call.request.header(HttpHeaders.Authorization) shouldContain "token"
            call.request.header("Behandlingsnummer") shouldBe pdlBehandlingsnummer

            if (config.hasEmptyResponse) {
                call.respondRawJson(responseWithoutData())
            } else if (config.fornavn == null || config.etternavn == null) {
                call.respondRawJson(responseWithError())
            } else {
                call.respondRawJson(validResponse(config.fornavn, config.mellomnavn, config.etternavn))
            }
        }

        externalServices {
            hosts("http://pdl") {
                routing {
                    post("/graphql") {
                        if (handlerOverride != null) {
                            handlerOverride()
                        } else if (responseConfig != null) {
                            defaultHandler(responseConfig)
                        } else {
                            throw IllegalArgumentException("Må definere responseconfig eller alternativ handler")
                        }
                    }
                }

            }
        }
    }
}
