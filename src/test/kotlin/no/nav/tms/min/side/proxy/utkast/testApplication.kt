package no.nav.tms.min.side.proxy.utkast

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tms.min.side.proxy.arbeid.ArbeidConsumer
import no.nav.tms.min.side.proxy.config.jsonConfig
import no.nav.tms.min.side.proxy.config.mainModule
import no.nav.tms.min.side.proxy.dittnav.DittnavConsumer
import no.nav.tms.min.side.proxy.health.HealthService
import no.nav.tms.min.side.proxy.sykefravaer.SykefravaerConsumer

private const val testIssuer = "test-issuer"
private val jwtStub = JwtStub(testIssuer)
private val stubToken = jwtStub.createTokenFor("subject", "audience")

internal fun ApplicationTestBuilder.mockApi(
    corsAllowedOrigins: String = "*.nav.no",
    corsAllowedSchemes: String = "https",
    arbeidConsumer: ArbeidConsumer,
    dittnavConsumer: DittnavConsumer,
    sykefraværConsumer: SykefravaerConsumer,
    utkastConsumer: UtkastConsumer,
    httpClient: HttpClient
) = application {
    mainModule(
        corsAllowedOrigins = corsAllowedOrigins,
        corsAllowedSchemes = corsAllowedSchemes, healthService = HealthService(),
        arbeidConsumer = arbeidConsumer,
        dittnavConsumer = dittnavConsumer,
        sykefravaerConsumer = sykefraværConsumer,
        utkastConsumer = utkastConsumer,
        httpClient = httpClient,
    )
}

fun ApplicationTestBuilder.applicationHttpClient() =
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

