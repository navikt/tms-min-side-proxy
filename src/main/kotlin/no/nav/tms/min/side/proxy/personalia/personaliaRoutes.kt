package no.nav.tms.min.side.proxy.personalia

import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.tms.min.side.proxy.user

fun Route.navnRoutes(navnFetcher: NavnFetcher) {
    get("/personalia/navn") {
        navnFetcher.getNavn(call.user)
            .let { navn -> call.respond(Navn(navn)) }
    }

    get("/personalia/ident") {
        call.respond(Ident(call.user.ident))
    }

    get("/navn") {
        try {
            navnFetcher.getNavn(call.user)
                .let { navn -> call.respond(NavnAndIdent(navn, call.user.ident)) }
        } catch (e: HentNavnException) {
            call.respond(NavnAndIdent(navn = null, ident = call.user.ident))
        }
    }

    // Legacy rute som tidligere var egen tokenx-inngang
    get("/personalia") {
        try {
            navnFetcher.getNavn(call.user)
                .let { navn -> call.respond(NavnAndIdent(navn, call.user.ident)) }
        } catch (e: HentNavnException) {
            call.respond(NavnAndIdent(navn = null, ident = call.user.ident))
        }
    }
}

data class Navn(val navn: String)
data class Ident(val ident: String)

data class NavnAndIdent(
    val navn: String?,
    val ident: String
)
