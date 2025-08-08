package no.nav.tms.min.side.proxy

import io.getunleash.FakeUnleash
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.tms.min.side.proxy.TestParameters.Companion.getParameters
import no.nav.tms.token.support.idporten.sidecar.mock.LevelOfAssurance
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GetRoutesTest {
    private val testParametersMap =
        mapOf(
            "meldekort" to TestParameters("http://meldekort.test"),
            "personalia" to TestParameters("http://personalia.test"),
            "selector" to TestParameters("http://selector.test"),
            "aia" to TestParameters(
                baseUrl = "http://paw.test",
                headers = mapOf("Nav-Call-Id" to "dummy-call-id"),
                queryParams = mapOf(
                    "feature" to "aia.bruk-bekreft-reaktivering",
                    "fraOgMed" to "2020-01-01",
                    "listeparameter" to "[101404,7267261]"
                )
            )
        )


    @ParameterizedTest
    @ValueSource(strings = ["meldekort", "selector", "aia"])
    fun `proxy get api`(tjenestePath: String) = testApplication {

        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        val parameters = testParametersMap.getParameters(tjenestePath)
        val proxyRouteAssert = ProxyRouteAssertion(parameters = parameters, isNestedPath = false)
        val proxyNestedRouteAssert = ProxyRouteAssertion(parameters = parameters, isNestedPath = true)

        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient),
            navnFetcher = mockk(),
            personaliaFetcher = mockk()
        )

        initExternalServices(
            parameters.baseUrl,
            HttpRouteConfig("/destination", assertionsBlock = proxyRouteAssert::assertion),
            HttpRouteConfig("/nested/destination", assertionsBlock = proxyNestedRouteAssert::assertion),
            HttpRouteConfig(
                "/servererror",
                HttpStatusCode.InternalServerError,
                responseContent = "Feil med status: 500",
            )
        )

        client.authenticatedGet(urlString = "/$tjenestePath/destination", extraheaders = parameters.headers).assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe defaultTestContent
        }
        client.authenticatedGet(
            "/$tjenestePath/nested/destination",
            extraheaders = parameters.headers,
            queryParams = parameters.queryParams
        ).assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe defaultTestContent
        }

        client.authenticatedGet(
            "/$tjenestePath/doesnotexist",
            extraheaders = parameters.headers
        ).status shouldBe HttpStatusCode.NotFound
        client.authenticatedGet(
            "/$tjenestePath/servererror",
            extraheaders = parameters.headers
        ).status shouldBe HttpStatusCode.ServiceUnavailable
    }


    @Test
    fun healtApiTest() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient),
            navnFetcher = mockk(),
            personaliaFetcher = mockk()
        )

        client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK
        client.get("/internal/isReady").status shouldBe HttpStatusCode.OK
        client.get("/internal/ping").status shouldBe HttpStatusCode.OK
    }

    @Test
    fun featuretoggleApiTest() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        val unleash = FakeUnleash()
        unleash.enable("testtoggle")

        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient),
            unleash = unleash,
            navnFetcher = mockk(),
            personaliaFetcher = mockk()
        )

        client.get("/featuretoggles").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe """{"testtoggle":true}"""
        }
    }

    @Test
    fun authPing() = testApplication {
        mockApi(
            contentFetcher = mockk(),
            externalContentFetcher = mockk(),
            navnFetcher = mockk(),
            personaliaFetcher = mockk()
        )
        client.get("/authPing").status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `Blokker loa-substantial for aia-kall`() {
        testApplication {
            mockApi(
                contentFetcher = mockk(),
                externalContentFetcher = mockk(),
                navnFetcher = mockk(),
                personaliaFetcher = mockk(),
                levelOfAssurance = LevelOfAssurance.SUBSTANTIAL
            )

            client.get("aia/er-arbeidsoker").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    private fun contentFecther(proxyHttpClient: ProxyHttpClient): ContentFetcher = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        selectorClientId = "selector",
        selectorBaseUrl = testParametersMap.getParameters("selector").baseUrl,
    )

    private fun externalContentFetcher(proxyHttpClient: ProxyHttpClient) = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        meldekortClientId = "meldekort",
        meldekortBaseUrl = testParametersMap.getParameters("meldekort").baseUrl,
        aiaBaseUrl = testParametersMap.getParameters("aia").baseUrl,
        aiaClientId = "aia"
    )

    private class ProxyRouteAssertion(
        private val parameters: TestParameters, private val isNestedPath: Boolean
    ) {
        fun assertion(it: ApplicationCall) {
            parameters.headers?.forEach { requiredHeader ->
                it.request.headers[requiredHeader.key] shouldBe requiredHeader.value
            }

            if (isNestedPath) {
                parameters.queryParams?.forEach { (name, value) ->
                    it.request.queryParameters[name] shouldBe value
                }
            }
        }
    }
}

private fun ApplicationTestBuilder.initExternalServices(
    host: String,
    vararg handlers: HttpRouteConfig
) {
    externalServices {
        hosts(host) {
            routing {
                handlers.forEach(::initService)
            }
        }
    }
}

private fun Routing.initService(routeConfig: HttpRouteConfig) {
    route(routeConfig.path) {
        method(routeConfig.method) {
            handle {
                routeConfig.assertionsBlock.invoke(call)
                call.respondText(routeConfig.responseContent, status = routeConfig.requestStatusCode, contentType = routeConfig.contentType)
            }
        }
    }
}

private class HttpRouteConfig(
    val path: String,
    val requestStatusCode: HttpStatusCode = HttpStatusCode.OK,
    val method: HttpMethod = HttpMethod.Get,
    val responseContent: String = defaultTestContent,
    val contentType: ContentType = ContentType.Application.Json,
    val assertionsBlock: suspend (ApplicationCall) -> Unit = {},
)
