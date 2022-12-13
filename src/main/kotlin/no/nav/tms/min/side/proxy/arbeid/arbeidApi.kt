package no.nav.tms.min.side.proxy.arbeid


import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.util.pipeline.PipelineContext
import no.nav.tms.min.side.proxy.authentication.IdportenUser
import no.nav.tms.min.side.proxy.authentication.IdportenUserFactory

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

