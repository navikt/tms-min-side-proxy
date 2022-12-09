package no.nav.tms.min.side.proxy.config

import no.nav.personbruker.dittnav.common.util.config.StringEnvVar.getEnvVar

data class Environment(
    val corsAllowedOrigins: String = getEnvVar("CORS_ALLOWED_ORIGINS"),
    val corsAllowedSchemes: String = getEnvVar("CORS_ALLOWED_SCHEMES"),
    val arbeidApiBaseUrl: String = getEnvVar("ARBEID_API_URL"),
    val arbeidApiClientId: String = getEnvVar("ARBEID_API_CLIENT_ID"),
    val dittnavApiClientId: String = getEnvVar("DITTNAV_API_CLIENT_ID"),
    val dittnavApiBaseUrl: String = getEnvVar("DITTNAV_API_URL"),
    val sykefravaerApiClientId: String = getEnvVar("SYKEFRAVAER_API_CLIENT_ID"),
    val sykefravaerApiBaseUrl: String = getEnvVar("SYKEFRAVAER_API_URL"),
    val utkastClientId: String = getEnvVar("UTKAST_CLIENT_ID"),
    val utastBaseUrl: String = getEnvVar("UTKAST_BASE_URL"),
)
