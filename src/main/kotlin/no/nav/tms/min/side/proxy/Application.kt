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

    private val appEnvResolver = AppEnvResolver(StringEnvVar.getEnvVar("NAIS_CLUSTER_NAME"))

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
        eventAggregator = appEnvResolver.variablesFor("dittnav-event-aggregator"),
        utkast = appEnvResolver.variablesFor("tms-utkast"),
        personalia = appEnvResolver.variablesFor(
            application = "tms-personalia-api",
            baseUrlPostfix = "/tms-personalia-api"
        ),
        selector = appEnvResolver.variablesFor("tms-mikrofrontend-selector"),
        varsel = appEnvResolver.variablesFor("tms-varsel-api"),
        statistikk = appEnvResolver.variablesFor("http://tms-statistikk"),

        )

    val externalContentFetcher = ExternalContentFetcher(
        proxyHttpClient = proxyHttpClient,
        aap = appEnvResolver.variablesFor(application = "soknad-api", namespace = "aap"),
        meldekort = appEnvResolver.variablesFor(
            application = meldekortApplication,
            namespace = "meldekort",
            baseUrlPostfix = "/meldekort/meldekort-api"
        ),
        sykDialogmote = appEnvResolver.variablesFor(application = "isdialogmote", namespace = "teamsykefravr"),
        aia = appEnvResolver.variablesFor(application = "aia-backend", namespace = "paw")
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