package no.nav.tms.min.side.proxy

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tms.token.support.idporten.sidecar.LevelOfAssurance
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

private val log = KotlinLogging.logger {}
private val securelog = KotlinLogging.logger("secureLog")
fun Route.proxyRoutes(contentFetcher: ContentFetcher, externalContentFetcher: ExternalContentFetcher) {

    get("/meldekort/{proxyPath...}") {
        val response = externalContentFetcher.getMeldekortContent(accessToken, proxyPath)
        call.respondBytes(response.readRawBytes(), response.contentType(), response.status)
    }

    get("/selector/{proxyPath...}") {
        val response = contentFetcher.getProfilContent(accessToken, proxyPath)
        call.respondBytes(response.readRawBytes(), response.contentType(), response.status)
    }

    get("/oppfolging") {
        val response = contentFetcher.getOppfolgingContent(accessToken, "api/niva3/underoppfolging")
        call.respondBytes(response.readRawBytes(), response.contentType(), response.status)
    }

}

fun Route.aiaRoutes(externalContentFetcher: ExternalContentFetcher) {
    get("/aia/{proxyPath...}") {
        if (levelOfAssurance == LevelOfAssurance.SUBSTANTIAL) {
            call.respond(HttpStatusCode.Unauthorized)
        } else {
            val response =
                externalContentFetcher.getAiaContent(accessToken, "$proxyPath$queryParameters", call.navCallId())
            call.respondBytes(response.readRawBytes(), response.contentType(), response.status)
        }


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


private val RoutingContext.accessToken
    get() = IdportenUserFactory.createIdportenUser(call).tokenString

private val RoutingContext.levelOfAssurance
    get() = IdportenUserFactory.createIdportenUser(call).levelOfAssurance

private val RoutingContext.ident
    get() = IdportenUserFactory.createIdportenUser(call).ident

private val RoutingContext.proxyPath: String?
    get() = call.parameters.getAll("proxyPath")?.joinToString("/")
private val RoutingContext.queryParameters: String
    get() = call.request.uri.split("?").let { pathsplit ->
        if (pathsplit.size > 1)
            "?${pathsplit[1]}"
        else ""
    }
