package no.nav.tms.min.side.proxy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as clientContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.*
import io.ktor.server.response.respondBytes
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tms.min.side.proxy.personalia.NavnFetcher
import no.nav.tms.token.support.azure.exchange.AzureService
import no.nav.tms.token.support.idporten.sidecar.mock.LevelOfAssurance
import no.nav.tms.token.support.idporten.sidecar.mock.idPortenMock
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import java.lang.IllegalArgumentException

private const val testIssuer = "test-issuer"
private val jwtStub = JwtStub(testIssuer)
private val stubToken = jwtStub.createTokenFor("subject", "audience")

internal fun ApplicationTestBuilder.mockApi(
    corsAllowedOrigins: String = "*.nav.no",
    corsAllowedSchemes: String = "https",
    contentFetcher: ContentFetcher,
    externalContentFetcher: ExternalContentFetcher,
    navnFetcher: NavnFetcher,
    levelOfAssurance: LevelOfAssurance = LevelOfAssurance.LEVEL_4,
    unleash: Unleash = FakeUnleash()
) = application {
    proxyApi(
        corsAllowedOrigins = corsAllowedOrigins,
        corsAllowedSchemes = corsAllowedSchemes,
        contentFetcher = contentFetcher,
        externalContentFetcher = externalContentFetcher,
        idportenAuthInstaller = {
            authentication {
                idPortenMock {
                    alwaysAuthenticated = true
                    setAsDefault = true
                    staticLevelOfAssurance = levelOfAssurance
                    staticUserPid = "12345"
                }
            }
        },
        navnFetcher = navnFetcher,
        unleash = unleash
    )
}


fun ApplicationTestBuilder.testApplicationHttpClient() =
    createClient {
        install(clientContentNegotiation) {
            jackson { jsonConfig() }
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

internal suspend fun HttpClient.authenticatedGet(
    urlString: String,
    token: String = stubToken,
    extraheaders: Map<String, String>? = null,
    queryParams: Map<String, String>? = null
): HttpResponse =
    request {
        url {
            url(urlString)
            queryParams?.forEach { name, value ->
                parameters.append(name, value)
            }
        }
        method = HttpMethod.Get
        header(HttpHeaders.Authorization, "Bearer $token")
        extraheaders?.forEach {
            header(it.key, it.value)
        }
    }

internal suspend fun HttpClient.authenticatedPost(
    urlString: String,
    token: String = stubToken,
    content: String = """{"test":"testcontent"}""",
    extraheaders: Map<String, String>? = null
): HttpResponse =
    request {
        url(urlString)
        method = HttpMethod.Post
        header(HttpHeaders.Cookie, "selvbetjening-idtoken=$token")
        extraheaders?.forEach {
            header(it.key, it.value)
        }
        setBody(content)
    }

const val defaultTestContent = """{"testinnhold": "her testes det innhold"}"""

val tokendigsMock = mockk<TokendingsService>().apply {
    coEvery { exchangeToken(any(), any()) } returns "<dummytoken>"
}

val azureMock = mockk<AzureService>().apply {
    coEvery { getAccessToken(any()) } returns "<azuretoken>"
}

fun checkJson(receiveText: String) {
    if (receiveText == "") throw AssertionError("Post kall har ikke sendt med body")
    try {
        jacksonObjectMapper().readTree(receiveText)
    } catch (_: Exception) {
        throw AssertionError("Post kall har sendt ugyldig json:\n$receiveText ")
    }
}

data class TestParameters(
    val baseUrl: String,
    val headers: Map<String, String>? = null,
    val queryParams: Map<String, String>? = null
) {
    companion object {
        fun Map<String, TestParameters>.getParameters(key: String) =
            get(key) ?: throw IllegalArgumentException("Finner ingen testparameter for $key")
    }
}
