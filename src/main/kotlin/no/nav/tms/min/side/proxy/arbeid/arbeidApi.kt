package no.nav.tms.min.side.proxy.arbeid

import io.ktor.application.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUser
import no.nav.tms.token.support.idporten.sidecar.user.IdportenUserFactory
import org.slf4j.LoggerFactory

fun Route.arbeidApi(consumer: ArbeidConsumer) {

    val log = LoggerFactory.getLogger(ArbeidConsumer::class.java)

    get("/arbeid/{proxyPath}") {
        val proxyPath = call.parameters["proxyPath"]

        try {
            val response = consumer.getContent(authenticatedUser, proxyPath)
            call.respond(response.status, response.readBytes())
        } catch (exception: Exception) {
            log.warn("Klarte ikke hente data fra '$proxyPath'. Feilmelding: ${exception.message}", exception)
            call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }

}

private val PipelineContext<Unit, ApplicationCall>.authenticatedUser: IdportenUser
    get() = IdportenUserFactory.createIdportenUser(call)

