package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.tms.min.side.proxy.TestParameters.Companion.getParameters
import org.junit.jupiter.api.Test

class GetRoutesTest {

    private val testParametersMap =
        mapOf(
            "selector" to TestParameters("http://selector.test"),
        )

    @Test
    fun `proxy get api`() = testApplication {

        val tjenestePath = "selector"

        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, userTokenExchangerMock, entraIdTokenFetcherMock)
        val parameters = testParametersMap.getParameters(tjenestePath)
        val proxyRouteAssert = ProxyRouteAssertion(parameters = parameters, isNestedPath = false)
        val proxyNestedRouteAssert = ProxyRouteAssertion(parameters = parameters, isNestedPath = true)

        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            navnFetcher = mockk(),
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
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, userTokenExchangerMock, entraIdTokenFetcherMock)
        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            navnFetcher = mockk(),
        )

        client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK
        client.get("/internal/isReady").status shouldBe HttpStatusCode.OK
        client.get("/internal/ping").status shouldBe HttpStatusCode.OK
    }

    @Test
    fun authPing() = testApplication {
        mockApi(
            contentFetcher = mockk(),
            navnFetcher = mockk()
        )
        client.get("/authPing").status shouldBe HttpStatusCode.OK
    }

    private fun contentFecther(proxyHttpClient: ProxyHttpClient): ContentFetcher = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        selectorClientId = "selector",
        selectorBaseUrl = testParametersMap.getParameters("selector").baseUrl,
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
