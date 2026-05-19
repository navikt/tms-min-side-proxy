package no.nav.tms.min.side.proxy

import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.statement.readRawBytes
import io.ktor.serialization.jackson.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.tms.common.logging.TeamLogs
import no.nav.tms.common.metrics.installTmsMicrometerMetrics
import no.nav.tms.common.observability.ApiMdc
import no.nav.tms.min.side.proxy.personalia.*
import no.nav.tms.token.support.user.login.routes.UserLoginRoutes
import no.nav.tms.token.support.user.token.verification.LevelOfAssurance
import no.nav.tms.token.support.user.token.verification.UserPrincipal
import no.nav.tms.token.support.user.token.verification.userToken

private val log = KotlinLogging.logger {}
private val teamLog = TeamLogs.logger { }

fun Application.proxyApi(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    contentFetcher: ContentFetcher,
    navnFetcher: NavnFetcher,
    authInstaller: Application.() -> Unit = {
        authentication {
            userToken {
                levelOfAssurance = LevelOfAssurance.Substantial
            }
        }
    }
) {
    val collectorRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(DefaultHeaders)
    install(ApiMdc)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.warn { "Api-kall feiler: ${cause.message}" }
            when (cause) {
                is TokendingsException -> {
                    teamLog.warn(cause) { "Feil ved veksling av token for baktjeneste" }
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }
                is MissingHeaderException -> {
                    call.respond(HttpStatusCode.BadRequest)
                }

                is RequestExcpetion -> {
                    call.respond(cause.responseCode)
                }

                is HentNavnException -> {
                    teamLog.warn(cause) { "Feil ved henting av navn" }
                    call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av navn")
                }

                else -> {
                    teamLog.error(cause) { "Uventet feil" }
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

        }
    }

    authInstaller()

    installTmsMicrometerMetrics {
        installMicrometerPlugin = true
        registry = collectorRegistry
    }

    install(CORS) {
        allowHost(host = corsAllowedOrigins, schemes = listOf(corsAllowedSchemes))
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
    }

    install(ContentNegotiation) {
        jackson { jsonConfig() }
    }

    install(UserLoginRoutes)

    routing {
        metaRoutes(collectorRegistry)
        authenticate {
            get("authPing") {
                call.respond(HttpStatusCode.OK)
            }
            get("/selector/{proxyPath...}") {
                val response = contentFetcher.getProfilContent(call.user.accessToken, proxyPath)
                call.respondBytes(response.readRawBytes(), response.contentType(), response.status)
            }
            navnRoutes(navnFetcher)
        }
    }

    configureShutdownHook(contentFetcher)
}

private fun Application.configureShutdownHook(contentFetcher: ContentFetcher) {
    monitor.subscribe(ApplicationStopping) {
        contentFetcher.shutDown()
    }
}

val RoutingCall.user get() = principal<UserPrincipal>() ?: throw IllegalStateException("Fant ikke UserPrincipal i context.")

private val RoutingContext.proxyPath: String?
    get() = call.parameters.getAll("proxyPath")?.joinToString("/")
