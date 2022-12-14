package no.nav.tms.min.side.proxy

import io.ktor.client.statement.readBytes
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import no.nav.tms.min.side.proxy.config.accessToken
import no.nav.tms.min.side.proxy.config.jsonConfig
import no.nav.tms.min.side.proxy.config.proxyPath

fun Route.proxyApi(contentFetcher: ContentFetcher) {

    get("/arbeid/{proxyPath...}") {
        val response = contentFetcher.getArbeidContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }

    get("/dittnav/{proxyPath...}") {
        val response = contentFetcher.getDittNavContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }

    post("/dittnav/{proxyPath...}") {
        val content = jsonConfig().parseToJsonElement(call.receiveText())
        val response = contentFetcher.postDittNavContent(accessToken, content, proxyPath)
        call.respond(response.status)
    }
    get("/sykefravaer/{proxyPath...}") {
        val response = contentFetcher.getSykefravaerContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }

    get("/utkast/{proxyPath...}") {
        val response = contentFetcher.getUtkastContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }
}



