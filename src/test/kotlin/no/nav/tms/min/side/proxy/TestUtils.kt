package no.nav.tms.min.side.proxy

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import no.nav.tms.common.testutils.HTTPVerb
import no.nav.tms.common.testutils.RouteProvider

class HttpRouteProvider(
    path: String,
    requestStatusCode: HttpStatusCode = HttpStatusCode.OK,
    assert: suspend (ApplicationCall) -> Unit = {},
    private val requestContent: String = defaultTestContent,
    routeMethodFunction: HTTPVerb = Routing::get,
) : RouteProvider(
    path = path,
    assert = assert,
    routeMethodFunction = routeMethodFunction,
    statusCode = requestStatusCode
) {
    override fun content() = this.requestContent
}

