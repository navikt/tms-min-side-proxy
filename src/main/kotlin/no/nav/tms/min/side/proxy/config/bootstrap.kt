package no.nav.tms.min.side.proxy.config


import com.auth0.jwk.JwkProvider
import com.auth0.jwt.interfaces.Payload
import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
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
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.util.pipeline.PipelineContext
import mu.KotlinLogging
import no.nav.tms.min.side.proxy.proxyApi
import no.nav.tms.min.side.proxy.ContentFetcher
import no.nav.tms.token.support.idporten.sidecar.LoginLevel.LEVEL_3
import no.nav.tms.token.support.idporten.sidecar.installIdPortenAuth
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

private val log = KotlinLogging.logger {}
fun Application.mainModule(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    httpClient: HttpClient,
    contentFetcher: ContentFetcher,
    idportenAuthInstaller: Application.() -> Unit = {
        installIdPortenAuth {
            setAsDefault = true
            loginLevel = LEVEL_3
        }
    }
) {

    install(DefaultHeaders)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.info {
                "Ukjent feil i proxy ${cause.message}"
            }
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    idportenAuthInstaller()
    install(CORS) {
        allowHost(host = corsAllowedOrigins, schemes = listOf(corsAllowedSchemes))
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
    }

    install(ContentNegotiation) {
        json(jsonConfig())
    }

    routing {
        healthApi()
        authenticate {
            proxyApi(contentFetcher)
        }
    }

    configureShutdownHook(httpClient)
}

private fun Application.configureShutdownHook(httpClient: HttpClient) {
    environment.monitor.subscribe(ApplicationStopping) {
        httpClient.close()
    }
}

internal val PipelineContext<Unit, ApplicationCall>.accessToken
    get() = IdportenUserFactory.createIdportenUser(call).tokenString

internal val PipelineContext<Unit, ApplicationCall>.proxyPath: String?
    get() = call.parameters.getAll("proxyPath")?.joinToString("/")