package no.nav.tms.min.side.proxy

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.common.util.config.StringEnvVar
import no.nav.tms.min.side.proxy.personalia.NavnFetcher
import no.nav.tms.token.support.entraid.token.fetcher.EntraIdTokenFetcherBuilder
import no.nav.tms.token.support.user.token.exchange.UserTokenExchangerBuilder

fun main() {
    embeddedServer(
        factory = Netty,
        module =  { envConfig(AppConfiguration()) },
        configure = { connector { port = 8080 } }
    ).start(wait = true)
}

data class AppConfiguration(
    val corsAllowedOrigins: String = StringEnvVar.getEnvVar("CORS_ALLOWED_ORIGINS"),
    val corsAllowedSchemes: String = StringEnvVar.getEnvVar("CORS_ALLOWED_SCHEMES"),
    private val selectorClientId: String = StringEnvVar.getEnvVar("SELCTOR_CLIENT_ID"),
    private val selectorBaseUrl: String = StringEnvVar.getEnvVar("SELCTOR_BASE_URL"),
    private val pdlApiClientId: String = StringEnvVar.getEnvVar("PDL_API_CLIENT_ID"),
    private val pdlApiUrl: String = StringEnvVar.getEnvVar("PDL_API_URL"),
    private val pdlBehandlingsnummer: String = StringEnvVar.getEnvVar("PDL_BEHANDLINGSNUMMER"),
) {
    private val httpClient = HttpClient(Apache5) {
        install(ContentNegotiation) {
            jackson {
                jsonConfig()
            }
        }
        install(HttpTimeout)
    }

    private val tokendingsService = UserTokenExchangerBuilder.build()

    private val proxyHttpClient = ProxyHttpClient(
        httpClient = httpClient,
        userTokenFetcher = tokendingsService,
        entraIdTokenFetcher = EntraIdTokenFetcherBuilder.build()
    )

    val contentFecther = ContentFetcher(
        proxyHttpClient = proxyHttpClient,
        selectorClientId = selectorClientId,
        selectorBaseUrl = selectorBaseUrl
    )

    val navnFetcher = NavnFetcher(
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

fun Application.envConfig(appConfig: AppConfiguration) {
    rootPath = "tms-min-side-proxy"
    proxyApi(
        corsAllowedOrigins = appConfig.corsAllowedOrigins,
        corsAllowedSchemes = appConfig.corsAllowedSchemes,
        contentFetcher = appConfig.contentFecther,
        navnFetcher = appConfig.navnFetcher,
    )
}
