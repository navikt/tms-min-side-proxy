package no.nav.tms.min.side.proxy.config


import io.ktor.client.HttpClient
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.routing.routing
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
        allowHost(host = corsAllowedOrigins, schemes = listOf(corsAllowedSchemes))
        allowCredentials = true
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Options)
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
