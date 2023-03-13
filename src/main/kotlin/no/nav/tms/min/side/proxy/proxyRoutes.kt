package no.nav.tms.min.side.proxy

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

fun Route.proxyRoutes(contentFetcher: ContentFetcher) {

    get("/aap/{proxyPath...}") {
        val response = contentFetcher.getAapContent(accessToken, proxyPath)
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

    post("/eventaggregator/{proxyPath...}") {
        val content = jsonConfig().parseToJsonElement(call.receiveText())
        val response = contentFetcher.postEventAggregatorContent(accessToken, content, proxyPath)
        call.respond(response.status)
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

    get("/selector/{proxyPath...}") {
        val response = contentFetcher.getProfilContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }

    get("/varsel/{proxyPath...}") {
        val response = contentFetcher.getVarselContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }

    post("/statistikk/innlogging") {
        contentFetcher.postInnloggingStatistikk(ident)
        call.respond(HttpStatusCode.OK)
    }
}


private val PipelineContext<Unit, ApplicationCall>.accessToken
    get() = IdportenUserFactory.createIdportenUser(call).tokenString

private val PipelineContext<Unit, ApplicationCall>.ident
    get() = IdportenUserFactory.createIdportenUser(call).ident

private val PipelineContext<Unit, ApplicationCall>.proxyPath: String?
    get() = call.parameters.getAll("proxyPath")?.joinToString("/")
