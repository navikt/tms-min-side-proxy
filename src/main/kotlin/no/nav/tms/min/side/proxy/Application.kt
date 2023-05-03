package no.nav.tms.min.side.proxy

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar
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
    val meldekortApplication: String = StringEnvVar.getEnvVar("MELDEKORT_APPLICATION"),
    private val oppfolgingClientId: String = StringEnvVar.getEnvVar("OPPFOLGING_CLIENT_ID"),
    private val oppfolgingBaseUrl: String = StringEnvVar.getEnvVar("OPPFOLGING_API_URL"),
) {

    private val appResolver = AppResolver(StringEnvVar.getEnvVar("NAIS_CLUSTER_NAME"))

    private val httpClient = HttpClient(Apache.create()) {
        install(ContentNegotiation) {
            json(jsonConfig())
        }
        install(HttpTimeout)
    }

    private val proxyHttpClient = ProxyHttpClient(
        httpClient = httpClient,
        tokendingsService = TokendingsServiceBuilder.buildTokendingsService(),
        azureService = AzureServiceBuilder.buildAzureService()
    )

    val contentFecther = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        oppfolgingClientId = oppfolgingClientId,
        oppfolgingBaseUrl = oppfolgingBaseUrl,
        eventAggregator = appResolver.variablesFor("dittnav-event-aggregator"),
        utkast = appResolver.variablesFor("tms-utkast"),
        personalia = appResolver.variablesFor(
            application = "tms-personalia-api",
            baseUrlPostfix = "/tms-personalia-api"
        ),
        selector = appResolver.variablesFor("tms-mikrofrontend-selector"),
        varsel = appResolver.variablesFor("tms-varsel-api"),
        statistikk = appResolver.variablesFor("http://tms-statistikk"),

        )

    val externalContentFetcher = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        aap = appResolver.variablesFor(application = "soknad-api", namespace = "aap"),
        meldekort = appResolver.variablesFor(
            application = meldekortApplication,
            namespace = "meldekort",
            baseUrlPostfix = "/meldekort/meldekort-api"
        ),
        sykDialogmote = appResolver.variablesFor(application = "isdialogmote", namespace = "teamsykefravr"),
        aia = appResolver.variablesFor(application = "aia-backend", namespace = "paw")
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
            corsAllowedSchemes = "https",
            contentFetcher = appConfig.contentFecther,
            externalContentFetcher = appConfig.externalContentFetcher
        )
    }
    connector {
        port = 8080
    }
}