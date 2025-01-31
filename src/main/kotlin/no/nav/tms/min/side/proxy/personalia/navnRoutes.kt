package no.nav.tms.min.side.proxy.personalia

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory

fun Route.navnRoutes(navnFetcher: NavnFetcher) {
    get("/personalia/navn") {
        navnFetcher.getNavn(user)
            .let { navn -> call.respond(Navn(navn)) }
    }

    get("/personalia/ident") {
        call.respond(Ident(user.ident))
    }

    get("/navn") {
        try {
            navnFetcher.getNavn(user)
                .let { navn -> call.respond(NavnAndIdent(navn, user.ident)) }
        } catch (e: HentNavnException) {
            call.respond(NavnAndIdent(navn = null, ident = user.ident))
        }
    }
}

data class Navn(val navn: String)
data class Ident(val ident: String)

data class NavnAndIdent(
    val navn: String?,
    val ident: String
)

private val RoutingContext.user
    get() = IdportenUserFactory.createIdportenUser(call)
