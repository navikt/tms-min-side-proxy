package no.nav.tms.min.side.proxy

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
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class GetRoutesTest {

    private val testParametersMap =
        mapOf(
            "aap" to TestParameters("http://aap.test"),
            "meldekort" to TestParameters("http://meldekort.test"),
            "utkast" to TestParameters("http://utkast.test"),
            "personalia" to TestParameters("http://personalia.test"),
            "selector" to TestParameters("http://selector.test"),
            "varsel" to TestParameters("http://varsel.test"),
            "eventaggregator" to TestParameters("http://eventAggregator.test"),
            "syk/dialogmote" to TestParameters("http://isdialog.test"),
            "oppfolging" to TestParameters("http://veilarboppfolging.test"),
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
    @ValueSource(strings = ["aap", "utkast", "personalia", "meldekort", "selector", "varsel", "syk/dialogmote", "aia"])
    fun `proxy get api`(tjenestePath: String) = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        val parameters = testParametersMap.getParameters(tjenestePath)

        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        externalServices {
            hosts(parameters.baseUrl) {
                routing {
                    get("/destination") {
                        parameters.headers?.forEach { requiredHeader ->
                            call.request.headers[requiredHeader.key] shouldBe requiredHeader.value
                        }
                        call.respondRawJson(defaultTestContent)
                    }
                    get("/nested/destination") {
                        parameters.headers?.forEach { requiredHeader ->
                            call.request.headers[requiredHeader.key] shouldBe requiredHeader.value
                        }
                        parameters.queryParams?.forEach { (name, value) ->
                            call.request.queryParameters[name] shouldBe value
                        }
                        call.respondRawJson(defaultTestContent)
                    }
                    get("/servererror") {

                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

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
    fun oppfolging() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        val url = "oppfolging"
        val testParameters = testParametersMap.getParameters("oppfolging")

        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        externalServices {
            hosts(testParameters.baseUrl) {
                routing {
                    get("/api/niva3/underoppfolging") {
                        val navconsumerHeader = call.request.header("Nav-Consumer-Id")
                        if (navconsumerHeader == null) {
                            call.respond(HttpStatusCode.BadRequest)
                        } else {
                            navconsumerHeader shouldBe "min-side:tms-min-side-proxy"
                            call.respondRawJson(defaultTestContent)
                        }

                    }
                }
            }
        }

        client.authenticatedGet("/$url").assert {
            status shouldBe HttpStatusCode.OK
            bodyAsText() shouldBe defaultTestContent
        }
    }

    @Test
    fun healtApiTest() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK
        client.get("/internal/isReady").status shouldBe HttpStatusCode.OK
        client.get("/internal/ping").status shouldBe HttpStatusCode.OK
    }

    @Test
    fun authPing() = testApplication {
        mockApi(contentFetcher = mockk(), externalContentFetcher = mockk())
        client.get("/authPing").status shouldBe HttpStatusCode.OK
    }

    private fun contentFecther(proxyHttpClient: ProxyHttpClient): ContentFetcher = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        eventAggregator = Pair("eventaggregatorclient", testParametersMap.getParameters("eventaggregator").baseUrl),
        utkast = Pair("utkastclient", testParametersMap.getParameters("utkast").baseUrl),
        personalia = Pair("personalia", testParametersMap.getParameters("personalia").baseUrl),
        selector = Pair("selector",testParametersMap.getParameters("selector").baseUrl),
        varsel = Pair("varsel", testParametersMap.getParameters("varsel").baseUrl),
        statistikk = Pair("statistikk", "http://statistikk.test"),
        oppfolgingBaseUrl = testParametersMap.getParameters("oppfolging").baseUrl,
        oppfolgingClientId = "veilarboppfolging"
    )

    private fun externalContentFetcher(proxyHttpClient: ProxyHttpClient) = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        aap = Pair("aap", testParametersMap.getParameters("aap").baseUrl),
        meldekort = Pair("meldekort",testParametersMap.getParameters("meldekort").baseUrl),
        sykDialogmote = Pair("sykdialogmote", testParametersMap.getParameters("syk/dialogmote").baseUrl),
        aia = Pair("aia", testParametersMap.getParameters("aia").baseUrl)
    )
}
