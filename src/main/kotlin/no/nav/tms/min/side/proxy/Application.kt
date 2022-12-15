package no.nav.tms.min.side.proxy

import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import no.nav.tms.min.side.proxy.config.Environment
import no.nav.tms.min.side.proxy.config.HttpClientBuilder
import no.nav.tms.min.side.proxy.config.mainModule

private val log = KotlinLogging.logger {  }
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
    }.start(wait = true).also {
        log.info { "Starter applikasjon med config ${it.environment.config.toMap()}" }
    }
}

