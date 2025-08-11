package no.nav.tms.min.side.proxy

import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

fun Route.proxyRoutes(contentFetcher: ContentFetcher, externalContentFetcher: ExternalContentFetcher) {

    get("/meldekort/{proxyPath...}") {
        val response = externalContentFetcher.getMeldekortContent(accessToken, proxyPath)
        call.respondBytes(response.readRawBytes(), response.contentType(), response.status)
    }

    get("/selector/{proxyPath...}") {
        val response = contentFetcher.getProfilContent(accessToken, proxyPath)
        call.respondBytes(response.readRawBytes(), response.contentType(), response.status)
    }
}

private val RoutingContext.accessToken
    get() = IdportenUserFactory.createIdportenUser(call).tokenString

private val RoutingContext.proxyPath: String?
    get() = call.parameters.getAll("proxyPath")?.joinToString("/")
private val RoutingContext.queryParameters: String
    get() = call.request.uri.split("?").let { pathsplit ->
        if (pathsplit.size > 1)
            "?${pathsplit[1]}"
        else ""
    }
