package no.nav.tms.min.side.proxy.utkast

import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.util.pipeline.PipelineContext
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUser
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory
import org.slf4j.LoggerFactory


fun Route.utkastApi(consumer: UtkastConsumer) {

    val log = LoggerFactory.getLogger(UtkastConsumer::class.java)

    get("/utkast") {
        try {
            val response = consumer.getContent(authenticatedUser, "utkast")
            call.respond(response.status, response.readBytes())
        } catch (exception: Exception) {
            log.warn("Klarte ikke hente data fra 'utkast'. Feilmelding: ${exception.message}", exception)
            call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }
    get("/utkast/{proxyPath}") {
        try {
            val proxyPath = call.parameters["proxyPath"]
            val response = consumer.getContent(authenticatedUser, "utkast/$proxyPath")
            call.respond(response.status, response.readBytes())
        } catch (exception: Exception) {
            log.warn("Klarte ikke hente data fra 'utkast'. Feilmelding: ${exception.message}", exception)
            call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }

}

private val PipelineContext<Unit, ApplicationCall>.authenticatedUser: IdportenUser
    get() = IdportenUserFactory.createIdportenUser(call)

