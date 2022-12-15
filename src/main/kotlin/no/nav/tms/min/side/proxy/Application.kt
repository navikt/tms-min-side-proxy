package no.nav.tms.min.side.proxy

import io.ktor.client.HttpClient
import io.ktor.server.application.port
import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mu.KotlinLogging
import no.nav.tms.min.side.proxy.config.Environment
import no.nav.tms.min.side.proxy.config.HttpClientBuilder
import no.nav.tms.min.side.proxy.config.mainModule

private val log = KotlinLogging.logger { }
fun main() {
    val env = Environment()
    val httpClient = HttpClientBuilder.build()

    val envConfig = applicationEngineEnvironment { envConfig(env, httpClient) }
    embeddedServer(factory = Netty,environment = envConfig).start(wait = true).also {
        println("starter med portconfig \n ${it.environment.config.port}")
        println("starter med rootpah \n ${it.environment.rootPath}")
    }
}

fun ApplicationEngineEnvironmentBuilder.envConfig(env: Environment, httpClient: HttpClient) {
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
