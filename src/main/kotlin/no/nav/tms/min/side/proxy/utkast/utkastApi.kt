package no.nav.tms.min.side.proxy.utkast

import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.util.pipeline.PipelineContext
import no.nav.tms.min.side.proxy.common.ContentFetcher
import no.nav.tms.min.side.proxy.config.accessToken
import no.nav.tms.min.side.proxy.config.proxyPath
import org.slf4j.LoggerFactory


fun Route.utkastApi(contentFetcher: ContentFetcher) {

    get("/utkast/{proxyPath...}") {
            val response = contentFetcher.getUtkastContent(accessToken, proxyPath)
            call.respond(response.status, response.readBytes())
    }
}



