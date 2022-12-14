package no.nav.tms.min.side.proxy

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.min.side.proxy.common.ContentFetcher
import no.nav.tms.min.side.proxy.config.Environment
import no.nav.tms.min.side.proxy.config.HttpClientBuilder
import no.nav.tms.min.side.proxy.config.LoginserviceMetadata
import no.nav.tms.min.side.proxy.config.mainModule
import no.nav.tms.min.side.proxy.health.HealthService
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import java.net.URL
import java.util.concurrent.TimeUnit

fun main() {
    val env = Environment()

    val httpClient = HttpClientBuilder.build()

    val tokendingsService = TokendingsServiceBuilder.buildTokendingsService()
    val contentFetcher = ContentFetcher(
        tokendingsService = tokendingsService,
        arbeidClientId = env.arbeidApiClientId,
        arbeidBaseUrl= env.arbeidApiBaseUrl,
        dittnavClientId = env.dittnavApiClientId,
        dittnavBaseUrl = env.dittnavApiBaseUrl,
        sykefravaerClientId = env.sykefravaerApiClientId,
        sykefravaerBaseUrl = env.sykefravaerApiBaseUrl,
        utkastClientId = env.utkastClientId,
        utkastBaseUrl = env.utkastBaseUrl,
        httpClient = httpClient
    )
    val loginserviceMetadata =
        LoginserviceMetadata.get(httpClient,env.loginserviceDiscoveryUrl)
    val jwkProvider = JwkProviderBuilder(URL(loginserviceMetadata.jwks_uri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()


    embeddedServer(Netty, port = 8080) {
        mainModule(
            corsAllowedOrigins = env.corsAllowedOrigins,
            corsAllowedSchemes = env.corsAllowedSchemes,
            healthService = HealthService(),
            httpClient = httpClient,
            jwkProvider = jwkProvider,
            jwtIssuer =  loginserviceMetadata.issuer,
            jwtAudience = env.loginserviceIdportenAudience,
            contentFetcher = contentFetcher
        )
    }.start(wait = true)
}