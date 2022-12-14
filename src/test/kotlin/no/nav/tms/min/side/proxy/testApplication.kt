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
import no.nav.tms.min.side.proxy.common.ContentFetcher
import no.nav.tms.min.side.proxy.config.jsonConfig
import no.nav.tms.min.side.proxy.config.mainModule
import no.nav.tms.min.side.proxy.health.HealthService
import no.nav.tms.min.side.proxy.utkast.JwtStub

private const val testIssuer = "test-issuer"
private val jwtStub = JwtStub(testIssuer)
private val stubToken = jwtStub.createTokenFor("subject", "audience")

internal fun ApplicationTestBuilder.mockApi(
    corsAllowedOrigins: String = "*.nav.no",
    corsAllowedSchemes: String = "https",
    httpClient: HttpClient = mockk(),
    contentFetcher : ContentFetcher
) = application {
    mainModule(
        corsAllowedOrigins = corsAllowedOrigins,
        corsAllowedSchemes = corsAllowedSchemes, healthService = HealthService(),
        httpClient = httpClient,
        jwkProvider = jwtStub.stubbedJwkProvider(),
        jwtIssuer = testIssuer,
        jwtAudience = "audience",
        contentFetcher = contentFetcher
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