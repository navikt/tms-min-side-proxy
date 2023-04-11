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
import no.nav.tms.token.support.azure.exchange.AzureService
import no.nav.tms.token.support.azure.exchange.AzureServiceBuilder
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
    private val aapBaseUrl: String = StringEnvVar.getEnvVar("AAP_BASE_URL"),
    private val aapClientId: String = StringEnvVar.getEnvVar("AAP_CLIENT_ID"),
    private val eventAggregatorClientId: String = StringEnvVar.getEnvVar("EVENT_AGGREGATOR_CLIENT_ID"),
    private val eventAggregatorBaseUrl: String = StringEnvVar.getEnvVar("EVENT_AGGREGATOR_BASE_URL"),
    private val meldekortClientId: String = StringEnvVar.getEnvVar("MELDEKORT_CLIENT_ID"),
    private val meldekortBaseUrl: String = StringEnvVar.getEnvVar("MELDEKORT_BASE_URL"),
    private val utkastClientId: String = StringEnvVar.getEnvVar("UTKAST_CLIENT_ID"),
    private val utkastBaseUrl: String = StringEnvVar.getEnvVar("UTKAST_BASE_URL"),
    private val personaliaClientId: String = StringEnvVar.getEnvVar("PERSONALIA_CLIENT_ID"),
    private val personaliaBaseUrl: String = StringEnvVar.getEnvVar("PERSONALIA_BASE_URL"),
    private val selectorClientId: String = StringEnvVar.getEnvVar("SELCTOR_CLIENT_ID"),
    private val selectorBaseUrl: String = StringEnvVar.getEnvVar("SELCTOR_BASE_URL"),
    private val varselClientId: String = StringEnvVar.getEnvVar("VARSEL_CLIENT_ID"),
    private val varselBaseUrl: String = StringEnvVar.getEnvVar("VARSEL_BASE_URL"),
    private val statistikkClientId: String = StringEnvVar.getEnvVar("STATISTIKK_CLIENT_ID"),
    private val statistikkBaseUrl: String = StringEnvVar.getEnvVar("STATISTIKK_BASE_URL"),
) {

    private val httpClient = HttpClient(Apache.create()) {
        install(ContentNegotiation) {
            json(jsonConfig())
        }
        install(HttpTimeout)
    }

    val contentFecther = ContentFetcher(
        tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
        azureService = AzureServiceBuilder.buildAzureService(),
        aapClientId = aapClientId,
        aapBaseUrl = aapBaseUrl,
        eventAggregatorClientId = eventAggregatorClientId,
        eventAggregatorBaseUrl = eventAggregatorBaseUrl,
        meldekortClientId = meldekortClientId,
        meldekortBaseUrl = meldekortBaseUrl,
        utkastClientId = utkastClientId,
        utkastBaseUrl = utkastBaseUrl,
        personaliaClientId = personaliaClientId,
        personaliaBaseUrl = personaliaBaseUrl,
        selectorClientId = selectorClientId,
        selectorBaseUrl = selectorBaseUrl,
        varselClientId = varselClientId,
        varselBaseUrl = varselBaseUrl,
        httpClient = httpClient,
        statistikkBaseApiUrl = statistikkBaseUrl,
        statistikkApiId = statistikkClientId
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
