package no.nav.tms.min.side.proxy

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.min.side.proxy.config.Environment
import no.nav.tms.min.side.proxy.config.HttpClientBuilder
import no.nav.tms.min.side.proxy.config.mainModule

fun main() {
    val env = Environment()
    val httpClient = HttpClientBuilder.build()

    embeddedServer(Netty, port = 8080) {
        mainModule(
            corsAllowedOrigins = env.corsAllowedOrigins,
            corsAllowedSchemes = env.corsAllowedSchemes,
            httpClient = httpClient,
            contentFetcher = env.contentFecther(httpClient)
        )
    }.start(wait = true)
}