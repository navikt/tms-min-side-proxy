package no.nav.tms.min.side.proxy

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.min.side.proxy.arbeid.ArbeidConsumer
import no.nav.tms.min.side.proxy.common.TokenFetcher
import no.nav.tms.min.side.proxy.config.Environment
import no.nav.tms.min.side.proxy.config.HttpClientBuilder
import no.nav.tms.min.side.proxy.config.LoginserviceMetadata
import no.nav.tms.min.side.proxy.config.mainModule
import no.nav.tms.min.side.proxy.dittnav.DittnavConsumer
import no.nav.tms.min.side.proxy.health.HealthService
import no.nav.tms.min.side.proxy.sykefravaer.SykefravaerConsumer
import no.nav.tms.min.side.proxy.utkast.UtkastConsumer
import no.nav.tms.token.support.tokendings.exchange.TokendingsServiceBuilder
import java.net.URL
import java.util.concurrent.TimeUnit

fun main() {
    val env = Environment()

    val httpClient = HttpClientBuilder.build()

    val tokendingsService = TokendingsServiceBuilder.buildTokendingsService()
    val tokenFetcher = TokenFetcher(
        tokendingsService = tokendingsService,
        arbeidClientId = env.arbeidApiClientId,
        dittnavClientId = env.dittnavApiClientId,
        sykefravaerClientId = env.sykefravaerApiClientId,
        ukastClientId = env.utkastClientId
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
            arbeidConsumer = ArbeidConsumer(httpClient, tokenFetcher, env.arbeidApiBaseUrl),
            dittnavConsumer = DittnavConsumer(httpClient, tokenFetcher, env.dittnavApiBaseUrl),
            sykefravaerConsumer = SykefravaerConsumer(httpClient, tokenFetcher, env.sykefravaerApiBaseUrl),
            utkastConsumer = UtkastConsumer(httpClient,tokenFetcher, env.utastBaseUrl),
            httpClient = httpClient,
            jwkProvider = jwkProvider,
            jwtIssuer =  loginserviceMetadata.issuer,
            jwtAudience = env.loginserviceIdportenAudience
        )
    }.start(wait = true)
}