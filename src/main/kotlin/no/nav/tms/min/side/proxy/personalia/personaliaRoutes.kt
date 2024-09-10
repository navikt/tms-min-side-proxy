package no.nav.tms.min.side.proxy.personalia

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import no.nav.tms.token.support.tokenx.validation.user.TokenXUserFactory

fun Route.personaliaRoutes(personaliaFetcher: PersonaliaFetcher) {
    get("/personalia") {
        try {
            personaliaFetcher.getNavn(user)
                .let { navn -> call.respond(NavnAndIdent(navn, user.ident)) }
        } catch (e: HentNavnException) {
            call.respond(NavnAndIdent(navn = null, ident = user.ident))
        }
    }
}

private val PipelineContext<Unit, ApplicationCall>.user
    get() = TokenXUserFactory.createTokenXUser(call)
