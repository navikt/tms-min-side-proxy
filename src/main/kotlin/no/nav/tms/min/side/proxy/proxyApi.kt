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
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.serialization.jackson.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.tms.common.metrics.installTmsMicrometerMetrics
import no.nav.tms.token.support.idporten.sidecar.IdPortenLogin
import no.nav.tms.token.support.idporten.sidecar.LevelOfAssurance.SUBSTANTIAL
import no.nav.tms.token.support.idporten.sidecar.idPorten
import no.nav.tms.common.observability.ApiMdc
import no.nav.tms.min.side.proxy.personalia.*
import no.nav.tms.token.support.tokenx.validation.TokenXAuthenticator
import no.nav.tms.token.support.tokenx.validation.tokenX

private val log = KotlinLogging.logger {}
private val securelog = KotlinLogging.logger("secureLog")
fun Application.proxyApi(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    contentFetcher: ContentFetcher,
    externalContentFetcher: ExternalContentFetcher,
    navnFetcher: NavnFetcher,
    personaliaFetcher: PersonaliaFetcher,
    idportenAuthInstaller: Application.() -> Unit = {
        authentication {
            idPorten {
                setAsDefault = true
                levelOfAssurance = SUBSTANTIAL
            }
        }
        install(IdPortenLogin)
    },
    tokenXAuthInstaller: Application.() -> Unit = {
        authentication {
            tokenX {
                setAsDefault = false
            }
        }
    },
    unleash: Unleash
) {
    val collectorRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    install(DefaultHeaders)
    install(ApiMdc)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.warn { "Api-kall feiler: ${cause.message}" }
            when (cause) {
                is TokendingsException -> {
                    securelog.warn(cause) {
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
    tokenXAuthInstaller()

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
        authenticate(TokenXAuthenticator.name) {
            personaliaRoutes(personaliaFetcher)
        }
    }

    configureShutdownHook(contentFetcher)
}

private fun Application.configureShutdownHook(contentFetcher: ContentFetcher) {
    monitor.subscribe(ApplicationStopping) {
        contentFetcher.shutDown()
    }
}
