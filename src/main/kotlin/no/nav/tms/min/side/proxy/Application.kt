package no.nav.tms.min.side.proxy

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.serialization.json.Json
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder

fun main() {
    embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment { envConfig(AppConfiguration()) }
    ).start(wait = true)
}

data class AppConfiguration(
    val corsAllowedOrigins: String = StringEnvVar.getEnvVar("CORS_ALLOWED_ORIGINS"),
    val corsAllowedSchemes: String = StringEnvVar.getEnvVar("CORS_ALLOWED_SCHEMES"),
    private val arbeidApiBaseUrl: String = StringEnvVar.getEnvVar("ARBEID_API_URL"),
    private val arbeidApiClientId: String = StringEnvVar.getEnvVar("ARBEID_API_CLIENT_ID"),
    private val dittnavApiClientId: String = StringEnvVar.getEnvVar("DITTNAV_API_CLIENT_ID"),
    private val dittnavApiBaseUrl: String = StringEnvVar.getEnvVar("DITTNAV_API_URL"),
    private val sykefravaerApiClientId: String = StringEnvVar.getEnvVar("SYKEFRAVAER_API_CLIENT_ID"),
    private val sykefravaerApiBaseUrl: String = StringEnvVar.getEnvVar("SYKEFRAVAER_API_URL"),
    private val utkastClientId: String = StringEnvVar.getEnvVar("UTKAST_CLIENT_ID"),
    private val utkastBaseUrl: String = StringEnvVar.getEnvVar("UTKAST_BASE_URL"),
) {

    private val httpClient = HttpClient(Apache.create()) {
        install(ContentNegotiation) {
            json(jsonConfig())
        }
        install(HttpTimeout)
    }

    val contentFecther = ContentFetcher(
        tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
        arbeidClientId = arbeidApiClientId,
        arbeidBaseUrl = arbeidApiBaseUrl,
        dittnavClientId = dittnavApiClientId,
        dittnavBaseUrl = dittnavApiBaseUrl,
        sykefravaerClientId = sykefravaerApiClientId,
        sykefravaerBaseUrl = sykefravaerApiBaseUrl,
        utkastClientId = utkastClientId,
        utkastBaseUrl = utkastBaseUrl,
        httpClient = httpClient
    )
}

fun jsonConfig(ignoreUnknownKeys: Boolean = false): Json {
    return Json {
        this.ignoreUnknownKeys = ignoreUnknownKeys
        this.encodeDefaults = true
    }
}

fun ApplicationEngineEnvironmentBuilder.envConfig(appConfig: AppConfiguration) {
    rootPath = "tms-min-side-proxy"
    module {
        proxyApi(
            corsAllowedOrigins = appConfig.corsAllowedOrigins,
            corsAllowedSchemes = appConfig.corsAllowedSchemes,
            contentFetcher = appConfig.contentFecther
        )
    }
    connector {
        port = 8080
    }
}
