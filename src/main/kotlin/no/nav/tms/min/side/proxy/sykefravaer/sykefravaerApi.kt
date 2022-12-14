package no.nav.tms.min.side.proxy.sykefravaer

import io.ktor.client.statement.readBytes
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tms.min.side.proxy.common.ContentFetcher
import no.nav.tms.min.side.proxy.config.accessToken
import no.nav.tms.min.side.proxy.config.proxyPath

fun Route.sykefraverApi(contentFetcher: ContentFetcher) {

    get("/sykefravaer/{proxyPath...}") {
        val response = contentFetcher.getSykefravaerContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }

}


