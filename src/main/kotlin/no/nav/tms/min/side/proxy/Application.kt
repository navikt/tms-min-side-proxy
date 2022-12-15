package no.nav.tms.min.side.proxy

import io.ktor.client.HttpClient
import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import no.nav.tms.min.side.proxy.config.Environment
import no.nav.tms.min.side.proxy.config.HttpClientBuilder
import no.nav.tms.min.side.proxy.config.mainModule

fun main() {
    val env = Environment()
    val httpClient = HttpClientBuilder.build()

    embeddedServer(factory = Netty, port =  8080) {
        applicationEngineEnvironment {
            rootPath = "tms-min-side-proxy"
            module {
                mainModule(
                    corsAllowedOrigins = env.corsAllowedOrigins,
                    corsAllowedSchemes = env.corsAllowedSchemes,
                    httpClient = httpClient,
                    contentFetcher = env.contentFecther(httpClient)
                )
            }
        }
    }.start(wait = true)
}

