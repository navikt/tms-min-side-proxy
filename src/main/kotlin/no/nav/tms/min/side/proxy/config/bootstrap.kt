package no.nav.tms.min.side.proxy.config


import com.auth0.jwk.JwkProvider
import com.auth0.jwt.interfaces.Payload
import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import no.nav.tms.min.side.proxy.arbeid.ArbeidConsumer
import no.nav.tms.min.side.proxy.arbeid.arbeidApi
import no.nav.tms.min.side.proxy.dittnav.DittnavConsumer
import no.nav.tms.min.side.proxy.dittnav.dittnavApi
import no.nav.tms.min.side.proxy.health.HealthService
import no.nav.tms.min.side.proxy.health.healthApi
import no.nav.tms.min.side.proxy.sykefravaer.SykefravaerConsumer
import no.nav.tms.min.side.proxy.sykefravaer.sykefraverApi
import no.nav.tms.min.side.proxy.utkast.UtkastConsumer
import no.nav.tms.min.side.proxy.utkast.utkastApi

fun Application.mainModule(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    healthService: HealthService,
    arbeidConsumer: ArbeidConsumer,
    dittnavConsumer: DittnavConsumer,
    sykefravaerConsumer: SykefravaerConsumer,
    utkastConsumer: UtkastConsumer,
    httpClient: HttpClient,
    jwkProvider: JwkProvider,
    jwtIssuer: String,
    jwtAudience: String
) {

    install(DefaultHeaders)

    install(CORS) {
        allowHost(host = corsAllowedOrigins, schemes = listOf(corsAllowedSchemes))
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
    }

    install(ContentNegotiation) {
        json(jsonConfig())
    }


    install(Authentication) {
        jwt {
            verifier(jwkProvider, jwtIssuer) {
                withAudience(jwtAudience)
            }

            authHeader {
                val cookie = it.request.cookies["selvbetjening-idtoken"] ?: throw CookieNotSetException()
                HttpAuthHeader.Single("Bearer", cookie)
            }

            validate { credentials ->
                requireNotNull(credentials.payload.claims["pid"]) {
                    "Token må inneholde fødselsnummer i pid claim"
                }
                PrincipalWithTokenString(
                    accessToken = request.cookies["selvbetjening-idtoken"] ?: throw CookieNotSetException(),
                    payload = credentials.payload
                )
            }
        }
    }

    routing {
        healthApi(healthService)

        authenticate {
            arbeidApi(arbeidConsumer)
            dittnavApi(dittnavConsumer)
            sykefraverApi(sykefravaerConsumer)
            utkastApi(utkastConsumer)
        }
    }

    configureShutdownHook(httpClient)
}

private fun Application.configureShutdownHook(httpClient: HttpClient) {
    environment.monitor.subscribe(ApplicationStopping) {
        httpClient.close()
    }
}

class CookieNotSetException : Throwable() {}
data class PrincipalWithTokenString(val accessToken: String, val payload: Payload) : Principal
internal val PipelineContext<Unit, ApplicationCall>.accessToken: String
    get() = call.principal<PrincipalWithTokenString>()?.accessToken
        ?: throw Exception("Principal har ikke blitt satt for authentication context.")