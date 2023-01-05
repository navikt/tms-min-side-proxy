package no.nav.tms.min.side.proxy

import io.ktor.client.statement.readBytes
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

fun Route.proxyRoutes(contentFetcher: ContentFetcher) {

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

    get("/personalia/{proxyPath...}") {
        val response = contentFetcher.getPersonaliaContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }

    get("/meldekort/{proxyPath...}") {
        val response = contentFetcher.getMeldekortContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }
}


private val PipelineContext<Unit, ApplicationCall>.accessToken
    get() = IdportenUserFactory.createIdportenUser(call).tokenString

private val PipelineContext<Unit, ApplicationCall>.proxyPath: String?
    get() = call.parameters.getAll("proxyPath")?.joinToString("/")
