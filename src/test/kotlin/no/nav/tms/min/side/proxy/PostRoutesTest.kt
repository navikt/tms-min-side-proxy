package no.nav.tms.min.side.proxy

import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tms.min.side.proxy.TestParameters.Companion.getParameters
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PostRoutesTest {

    private val testParametersMap =
        mapOf(
            "eventaggregator" to TestParameters("http://eventAggregator.test"),
            "aia" to TestParameters("http://paw.test", mapOf("Nav-Call-Id" to "dummy-call-id"))
        )


    @ParameterizedTest
    @ValueSource(strings = ["eventaggregator", "aia"])
    fun `proxy post`(tjenestePath: String) = testApplication {
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
                    post("/destination") {
                        parameters.headers?.forEach { requiredHeader ->
                            call.request.headers[requiredHeader.key] shouldBe requiredHeader.value
                        }
                        checkJson(call.receiveText())
                        call.respond(HttpStatusCode.OK)
                    }
                    post("/nested/destination") {
                        parameters.headers?.forEach { requiredHeader ->
                            call.request.headers[requiredHeader.key] shouldBe requiredHeader.value
                        }
                        checkJson(call.receiveText())
                        call.respond(HttpStatusCode.OK)
                    }
                    post("/servererror") {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
            }
        }

        client.authenticatedPost(urlString = "/$tjenestePath/destination", extraheaders = parameters.headers).assert {
            status shouldBe HttpStatusCode.OK
        }
        client.authenticatedPost("/$tjenestePath/nested/destination", extraheaders = parameters.headers).assert {
            status shouldBe HttpStatusCode.OK
        }

        client.authenticatedPost(
            "/$tjenestePath/doesnotexist",
            extraheaders = parameters.headers
        ).status shouldBe HttpStatusCode.NotFound
        client.authenticatedPost(
            "/$tjenestePath/servererror",
            extraheaders = parameters.headers
        ).status shouldBe HttpStatusCode.ServiceUnavailable
    }


    @Test
    fun `post statistikk`() = testApplication {
        val applicationhttpClient = testApplicationHttpClient()
        var callCount = 0
        val proxyHttpClient = ProxyHttpClient(applicationhttpClient, tokendigsMock, azureMock)
        mockApi(
            contentFetcher = contentFecther(proxyHttpClient),
            externalContentFetcher = externalContentFetcher(proxyHttpClient)
        )

        externalServices {
            hosts("http://statistikk.test") {
                routing {
                    post("/innlogging") {
                        callCount += 1
                        call.receive<StatistikkPostRequest>().ident shouldBe "12345"
                        call.respond(HttpStatusCode.OK)
                    }
                }
            }
        }

        client.authenticatedPost("/statistikk/innlogging").assert {
            status shouldBe HttpStatusCode.OK
        }
        callCount shouldBe 1
    }

    private fun externalContentFetcher(proxyHttpClient: ProxyHttpClient) = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        aap = defaultPair,
        meldekort = defaultPair,
        sykDialogmote= defaultPair,
        aia = Pair("aia",testParametersMap.getParameters("aia").baseUrl)
    )

    private fun contentFecther(proxyHttpClient: ProxyHttpClient) = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        eventAggregator = Pair("eventaggregator", testParametersMap.getParameters("eventaggregator").baseUrl),
        utkast= defaultPair,
        personalia= defaultPair,
        selector = defaultPair,
        varsel = defaultPair,
        statistikk = Pair("statistikk","http://statistikk.test"),
        oppfolgingClientId = "",
        oppfolgingBaseUrl = ""
    )
}

private class StatistikkPostRequest(val ident: String)

private val defaultPair = Pair("","")