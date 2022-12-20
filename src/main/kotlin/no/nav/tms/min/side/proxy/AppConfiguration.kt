package no.nav.tms.min.side.proxy

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder

data class AppConfiguration(
    val corsAllowedOrigins: String = getEnvVar("CORS_ALLOWED_ORIGINS"),
    val corsAllowedSchemes: String = getEnvVar("CORS_ALLOWED_SCHEMES"),
    private val arbeidApiBaseUrl: String = getEnvVar("ARBEID_API_URL"),
    private val arbeidApiClientId: String = getEnvVar("ARBEID_API_CLIENT_ID"),
    private val dittnavApiClientId: String = getEnvVar("DITTNAV_API_CLIENT_ID"),
    private val dittnavApiBaseUrl: String = getEnvVar("DITTNAV_API_URL"),
    private val sykefravaerApiClientId: String = getEnvVar("SYKEFRAVAER_API_CLIENT_ID"),
    private val sykefravaerApiBaseUrl: String = getEnvVar("SYKEFRAVAER_API_URL"),
    private val utkastClientId: String = getEnvVar("UTKAST_CLIENT_ID"),
    private val utkastBaseUrl: String = getEnvVar("UTKAST_BASE_URL"),
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
        utkastBaseUrl = "$utkastBaseUrl/utkast",
        httpClient = httpClient
    )
}

fun jsonConfig(ignoreUnknownKeys: Boolean = false): Json {
    return Json {
        this.ignoreUnknownKeys = ignoreUnknownKeys
        this.encodeDefaults = true
    }
}