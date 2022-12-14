package no.nav.tms.min.side.proxy

import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.min.side.proxy.config.Environment
import no.nav.tms.min.side.proxy.config.HttpClientBuilder
import no.nav.tms.min.side.proxy.config.LoginserviceMetadata
import no.nav.tms.min.side.proxy.config.mainModule
import java.net.URL
import java.util.concurrent.TimeUnit

fun main() {
    val env = Environment()
    val httpClient = HttpClientBuilder.build()
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
            httpClient = httpClient,
            jwkProvider = jwkProvider,
            jwtIssuer =  loginserviceMetadata.issuer,
            jwtAudience = env.loginserviceIdportenAudience,
            contentFetcher = env.contentFecther(httpClient)
        )
    }.start(wait = true)
}