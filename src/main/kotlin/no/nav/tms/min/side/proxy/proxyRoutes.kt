package no.nav.tms.min.side.proxy

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.flattenForEach
import io.ktor.util.pipeline.*
import io.ktor.util.toMap
import mu.KotlinLogging
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

private val log = KotlinLogging.logger {}
private val securelog = KotlinLogging.logger("secureLog")
fun Route.proxyRoutes(contentFetcher: ContentFetcher, externalContentFetcher: ExternalContentFetcher) {

    get("/aap/{proxyPath...}") {
        val response = externalContentFetcher.getAapContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
    }
    get("syk/dialogmote/{proxyPath...}") {
        val response = externalContentFetcher.getSykDialogmoteContent(accessToken, proxyPath)
        call.respond(response.status, response.readBytes())
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
        val response = externalContentFetcher.getMeldekortContent(accessToken, proxyPath)
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

    get("/oppfolging") {
        val response = contentFetcher.getOppfolgingContent(accessToken, "api/niva3/underoppfolging")
        call.respond(response.status, response.readBytes())
    }
    post("/statistikk/innlogging") {
        contentFetcher.postInnloggingStatistikk(ident)
        call.respond(HttpStatusCode.OK)
    }
}

fun Route.aiaRoutes(externalContentFetcher: ExternalContentFetcher) {
    get("/aia/{proxyPath...}") {
        val response = externalContentFetcher.getAiaContent(accessToken, "$proxyPath$queryParameters", call.navCallId())
        call.respondBytes(
            status = response.status,
            contentType = response.contentType(),
            provider = { response.readBytes() }
        )
    }

    post("/aia/{proxyPath...}") {
        val content = jsonConfig().parseToJsonElement(call.receiveText())
        val response = externalContentFetcher.postAiaContent(
            accessToken,
            proxyPath,
            content,
            call.navCallId(),
        )
        call.respondBytes(
            status = response.status,
            contentType = response.contentType(),
            provider = { response.readBytes() }
        )
    }
}

private fun ApplicationCall.navCallId() = request.headers["Nav-Call-Id"].also {
    if (it == null) {
        log.info { "Fant ikke header Nav-Call-Id for kall til ${this.request.uri}" }
        val headerStr = this.request.headers.entries().map { headers ->
            "${headers.key}:${headers.value} "
        }
        securelog.info { "Fant ikke header Nav-Call-Id for kall til ${this.request.uri}, eksisterende headere er $headerStr" }
    }
}


private val PipelineContext<Unit, ApplicationCall>.accessToken
    get() = IdportenUserFactory.createIdportenUser(call).tokenString

private val PipelineContext<Unit, ApplicationCall>.ident
    get() = IdportenUserFactory.createIdportenUser(call).ident

private val PipelineContext<Unit, ApplicationCall>.proxyPath: String?
    get() = call.parameters.getAll("proxyPath")?.joinToString("/")
private val PipelineContext<Unit, ApplicationCall>.queryParameters: String
    get() = call.request.uri.split("?").let { pathsplit ->
        if (pathsplit.size > 1)
            "?${pathsplit[1]}"
        else ""
    }
