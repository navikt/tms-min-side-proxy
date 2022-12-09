package no.nav.tms.min.side.proxy.config

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.routing.*
import io.ktor.serialization.*
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
import no.nav.tms.token.support.idporten.sidecar.LoginLevel
import no.nav.tms.token.support.idporten.sidecar.installIdPortenAuth

fun Application.mainModule(
    corsAllowedOrigins: String,
    corsAllowedSchemes: String,
    healthService: HealthService,
    arbeidConsumer: ArbeidConsumer,
    dittnavConsumer: DittnavConsumer,
    sykefravaerConsumer: SykefravaerConsumer,
    utkastConsumer: UtkastConsumer,
    httpClient: HttpClient
) {

    install(DefaultHeaders)

    install(CORS) {
        host(host = corsAllowedOrigins, schemes = listOf(corsAllowedSchemes))
        allowCredentials = true
        header(HttpHeaders.ContentType)
        method(HttpMethod.Options)
    }

    install(ContentNegotiation) {
        json(jsonConfig())
    }

    installIdPortenAuth {
        setAsDefault = true
        loginLevel = LoginLevel.LEVEL_3
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
