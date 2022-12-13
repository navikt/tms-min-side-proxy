package no.nav.tms.min.side.proxy.authentication

import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal

data class IdportenUser (
    val ident: String,
    val loginLevel: Int,
    val token: String,
)

object IdportenUserFactory {

    private fun createIdportenUser(principal: PrincipalWithTokenString): IdportenUser {

        val ident: String = principal.payload.getClaim("pid").asString()
        val loginLevel =
            extractLoginLevel(principal.payload)
        return IdportenUser(ident, loginLevel, principal.accessToken)
    }

    fun createIdportenUser(call: ApplicationCall): IdportenUser {
        val principal = call.principal<PrincipalWithTokenString>()
            ?: throw Exception("Principal har ikke blitt satt for authentication context.")

        return createIdportenUser(principal)
    }

    private fun extractLoginLevel(payload: Payload): Int {

        return when (payload.getClaim("acr").asString()) {
            "Level3" -> 3
            "Level4" -> 4
            else -> throw Exception("Innloggingsnivå ble ikke funnet. Dette skal ikke kunne skje.")
        }
    }

}

data class PrincipalWithTokenString(val accessToken: String, val payload: Payload) : Principal