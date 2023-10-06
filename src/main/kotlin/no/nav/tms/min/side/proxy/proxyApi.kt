package no.nav.tms.min.side.proxy

import io.getunleash.Unleash
import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.*
import nav.no.tms.common.metrics.installTmsMicrometerMetrics
import no.nav.tms.min.side.proxy.personalia.HentNavnException
import no.nav.tms.min.side.proxy.personalia.NavnFetcher
import no.nav.tms.min.side.proxy.personalia.navnRoutes
import no.nav.tms.token.support.idporten.sidecar.IdPortenLogin
import no.nav.tms.token.support.idporten.sidecar.LevelOfAssurance.SUBSTANTIAL
import no.nav.tms.token.support.idporten.sidecar.idPorten

private val log = KotlinLogging.logger {}
private val securelog = KotlinLogging.logger("secureLog")
fun Application.proxyApi(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    contentFetcher: ContentFetcher,
    externalContentFetcher: ExternalContentFetcher,
    navnFetcher: NavnFetcher,
    idportenAuthInstaller: Application.() -> Unit = {
        authentication {
            idPorten {
                setAsDefault = true
                levelOfAssurance = SUBSTANTIAL
            }
        }
        install(IdPortenLogin)
    },
    unleash: Unleash
) {
    val collectorRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(DefaultHeaders)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.warn { "${call.request.uri}: ${cause.message}" }
            when (cause) {
                is TokendingsException -> {
                    securelog.warn {
                        """
                        ${cause.message} for token 
                        ${cause.accessToken}
                        """.trimIndent()
                    }
                    call.respond(HttpStatusCode.ServiceUnavailable)
                }

                is MissingHeaderException -> {
                    call.respond(HttpStatusCode.BadRequest)
                }

                is RequestExcpetion -> {
                    call.respond(cause.responseCode)

                }

                is HentNavnException -> {
                    call.respond(HttpStatusCode.InternalServerError, "Feil ved henting av navn")
                }

                else -> {
                    securelog.error { cause.stackTraceToString() }
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

        }
    }

    idportenAuthInstaller()

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

    routing {
        metaRoutes(collectorRegistry)
        authenticate {
            get("authPing") {
                call.respond(HttpStatusCode.OK)
            }
            proxyRoutes(contentFetcher, externalContentFetcher)
            aiaRoutes(externalContentFetcher)
            navnRoutes(navnFetcher)
            get("featuretoggles") {
                call.respond(
                    unleash.more().evaluateAllToggles().associate {
                        it.name to it.isEnabled
                    }
                )
            }
        }
    }

    configureShutdownHook(contentFetcher)
}

private fun Application.configureShutdownHook(contentFetcher: ContentFetcher) {
    environment.monitor.subscribe(ApplicationStopping) {
        contentFetcher.shutDown()
    }
}
