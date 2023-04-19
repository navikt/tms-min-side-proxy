package no.nav.tms.min.side.proxy


import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import mu.KotlinLogging
import no.nav.tms.token.support.idporten.sidecar.LoginLevel.LEVEL_3
import no.nav.tms.token.support.idporten.sidecar.installIdPortenAuth

private val log = KotlinLogging.logger {}
private val securelog = KotlinLogging.logger("secureLog")
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
    val collectorRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(DefaultHeaders)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.info { "${call.request.uri}: ${cause.message}" }
            when (cause) {
                is TokendingsException -> {
                    securelog.info {
                        """
                        ${cause.message} for token 
                        ${cause.accessToken}
                        """.trimIndent()
                    }
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }
                is RequestExcpetion -> {
                    call.respond(cause.responseCode)

                }
                else -> {
                    securelog.info { cause.stackTraceToString() }
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

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

    install(MicrometerMetrics) {
        registry = collectorRegistry
    }

    routing {
        metaRoutes(collectorRegistry)
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
