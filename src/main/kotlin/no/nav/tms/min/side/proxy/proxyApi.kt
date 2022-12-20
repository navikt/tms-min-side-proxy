package no.nav.tms.min.side.proxy


import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.tms.token.support.idporten.sidecar.LoginLevel.LEVEL_3
import no.nav.tms.token.support.idporten.sidecar.installIdPortenAuth

private val log = KotlinLogging.logger {}
fun Application.proxyApi(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
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
        healthRoutes()
        authenticate {
            proxyRoutes(contentFetcher)
        }
    }

    configureShutdownHook(contentFetcher)
}

private fun Application.configureShutdownHook(contentFetcher: ContentFetcher) {
    environment.monitor.subscribe(ApplicationStopping) {
        contentFetcher.shutDown()
    }
}