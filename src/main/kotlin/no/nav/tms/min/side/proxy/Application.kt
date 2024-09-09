package no.nav.tms.min.side.proxy

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.*
import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.common.util.config.StringEnvVar
import no.nav.tms.min.side.proxy.personalia.PersonaliaFetcher
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
     private val meldekortClientId: String = StringEnvVar.getEnvVar("MELDEKORT_CLIENT_ID"),
    private val meldekortBaseUrl: String = StringEnvVar.getEnvVar("MELDEKORT_BASE_URL"),
    private val utkastClientId: String = StringEnvVar.getEnvVar("UTKAST_CLIENT_ID"),
    private val utkastBaseUrl: String = StringEnvVar.getEnvVar("UTKAST_BASE_URL"),
    private val selectorClientId: String = StringEnvVar.getEnvVar("SELCTOR_CLIENT_ID"),
    private val selectorBaseUrl: String = StringEnvVar.getEnvVar("SELCTOR_BASE_URL"),
    private val statistikkClientId: String = StringEnvVar.getEnvVar("STATISTIKK_CLIENT_ID"),
    private val statistikkBaseUrl: String = StringEnvVar.getEnvVar("STATISTIKK_BASE_URL"),
    private val oppfolgingClientId: String = StringEnvVar.getEnvVar("OPPFOLGING_CLIENT_ID"),
    private val oppfolgingBaseUrl: String = StringEnvVar.getEnvVar("OPPFOLGING_API_URL"),
    private val aiaClientId: String = StringEnvVar.getEnvVar("AIA_CLIENT_ID"),
    private val aiaBaseUrl: String = StringEnvVar.getEnvVar("AIA_API_URL"),
    private val pdlApiClientId: String = StringEnvVar.getEnvVar("PDL_API_CLIENT_ID"),
    private val pdlApiUrl: String = StringEnvVar.getEnvVar("PDL_API_URL"),
    private val pdlBehandlingsnummer: String = StringEnvVar.getEnvVar("PDL_BEHANDLINGSNUMMER"),

    val unleashEnvironment: String = StringEnvVar.getEnvVar("UNLEASH_ENVIRONMENT"),
    val unleashServerApiUrl: String = StringEnvVar.getEnvVar("UNLEASH_SERVER_API_URL"),
    val unleashServerApiToken: String = StringEnvVar.getEnvVar("UNLEASH_SERVER_API_TOKEN"),
) {
    private val httpClient = HttpClient(Apache.create()) {
        install(ContentNegotiation) {
            jackson {
                jsonConfig()
            }
        }
        install(HttpTimeout)
    }

    private val tokendingsService = TokendingsServiceBuilder.buildTokendingsService()

    private val proxyHttpClient = ProxyHttpClient(
        httpClient = httpClient,
        tokendingsService = tokendingsService,
        azureService = AzureServiceBuilder.buildAzureService()
    )

    val contentFecther = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        utkastClientId = utkastClientId,
        utkastBaseUrl = utkastBaseUrl,
        selectorClientId = selectorClientId,
        selectorBaseUrl = selectorBaseUrl,
        statistikkClientId = statistikkClientId,
        statistikkBaseApiUrl = statistikkBaseUrl,
        oppfolgingClientId = oppfolgingClientId,
        oppfolgingBaseUrl = oppfolgingBaseUrl,
    )

    val externalContentFetcher = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        meldekortClientId = meldekortClientId,
        meldekortBaseUrl = meldekortBaseUrl,
        aiaBaseUrl = aiaBaseUrl,
        aiaClientId = aiaClientId
    )

    val personaliaFetcher = PersonaliaFetcher(
        httpClient,
        pdlApiUrl,
        pdlApiClientId,
        pdlBehandlingsnummer,
        tokendingsService
    )
}

fun ObjectMapper.jsonConfig() {
    registerKotlinModule()
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    enable(SerializationFeature.CLOSE_CLOSEABLE)
}

fun ApplicationEngineEnvironmentBuilder.envConfig(appConfig: AppConfiguration) {
    rootPath = "tms-min-side-proxy"
    module {
        proxyApi(
            corsAllowedOrigins = appConfig.corsAllowedOrigins,
            corsAllowedSchemes = appConfig.corsAllowedSchemes,
            contentFetcher = appConfig.contentFecther,
            externalContentFetcher = appConfig.externalContentFetcher,
            personaliaFetcher = appConfig.personaliaFetcher,
            unleash = setupUnleash(
                unleashApiUrl = appConfig.unleashServerApiUrl,
                unleashApiKey = appConfig.unleashServerApiToken,
                unleashEnvironment = appConfig.unleashEnvironment
            )
        )
    }
    connector {
        port = 8080
    }
}
