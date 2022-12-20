package no.nav.tms.min.side.proxy

import io.ktor.server.engine.ApplicationEngineEnvironmentBuilder
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val envConfig = applicationEngineEnvironment { envConfig(AppConfiguration()) }
    embeddedServer(factory = Netty,environment = envConfig).start(wait = true)
}

fun ApplicationEngineEnvironmentBuilder.envConfig(appConfig: AppConfiguration) {
    rootPath = "tms-min-side-proxy"
    module {
        proxyApi(
            corsAllowedOrigins = appConfig.corsAllowedOrigins,
            corsAllowedSchemes = appConfig.corsAllowedSchemes,
            contentFetcher = appConfig.contentFecther
        )
    }
    connector {
        port = 8080
    }
}
