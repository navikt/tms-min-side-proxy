package no.nav.tms.min.side.proxy

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondBytes
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.min.side.proxy.arbeid.ArbeidConsumer
import no.nav.tms.min.side.proxy.common.TokenFetcher
import no.nav.tms.min.side.proxy.config.jsonConfig
import no.nav.tms.min.side.proxy.config.mainModule
import no.nav.tms.min.side.proxy.dittnav.DittnavConsumer
import no.nav.tms.min.side.proxy.health.HealthService
import no.nav.tms.min.side.proxy.sykefravaer.SykefravaerConsumer
import no.nav.tms.min.side.proxy.utkast.JwtStub
import no.nav.tms.min.side.proxy.utkast.UtkastConsumer

private const val testIssuer = "test-issuer"
private val jwtStub = JwtStub(testIssuer)
private val stubToken = jwtStub.createTokenFor("subject", "audience")

internal fun ApplicationTestBuilder.mockApi(
    corsAllowedOrigins: String = "*.nav.no",
    corsAllowedSchemes: String = "https",
    arbeidConsumer: ArbeidConsumer = mockk(),
    dittnavConsumer: DittnavConsumer = mockk(),
    sykefraværConsumer: SykefravaerConsumer = mockk(),
    utkastConsumer: UtkastConsumer = mockk(),
    httpClient: HttpClient = mockk()
) = application {
    mainModule(
        corsAllowedOrigins = corsAllowedOrigins,
        corsAllowedSchemes = corsAllowedSchemes, healthService = HealthService(),
        arbeidConsumer = arbeidConsumer,
        dittnavConsumer = dittnavConsumer,
        sykefravaerConsumer = sykefraværConsumer,
        utkastConsumer = utkastConsumer,
        httpClient = httpClient,
        jwtAudience = "audience",
        jwkProvider = jwtStub.stubbedJwkProvider(),
        jwtIssuer = testIssuer
    )
}

fun ApplicationTestBuilder.testApplicationHttpClient() =
    createClient {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(jsonConfig())
        }
        install(HttpTimeout)
    }

internal inline fun <T> T.assert(block: T.() -> Unit): T =
    apply {
        block()
    }

internal suspend fun ApplicationCall.respondRawJson(content: String) =
    respondBytes(
        contentType = ContentType.Application.Json,
        provider = { content.toByteArray() })

internal suspend fun HttpClient.authenticatedGet(urlString: String, token: String = stubToken): HttpResponse = request {
    url(urlString)
    method = HttpMethod.Get
    header(HttpHeaders.Cookie, "selvbetjening-idtoken=$token")
}


internal val tokenfetcherMock = mockk<TokenFetcher>().also {
    coEvery { it.getUtkastApiToken(any()) } returns "<dummytoken>"
    coEvery { it.getArbeidApiToken(any()) } returns "<dummytoken>"
    coEvery { it.getDittnavApiToken(any()) } returns "<dummytoken>"
    coEvery { it.getSykefravaerApiToken(any()) } returns "<dummytoken>"
}