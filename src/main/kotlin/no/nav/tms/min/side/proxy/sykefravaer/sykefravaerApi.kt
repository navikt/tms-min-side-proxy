package no.nav.tms.min.side.proxy.sykefravaer


import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.tms.min.side.proxy.config.accessToken
import org.slf4j.LoggerFactory

fun Route.sykefraverApi(consumer: SykefravaerConsumer) {

    val log = LoggerFactory.getLogger(SykefravaerConsumer::class.java)

    get("/sykefravaer/{proxyPath}") {
        val proxyPath = call.parameters["proxyPath"]

        try {
            val response = consumer.getContent(accessToken, proxyPath)
            call.respond(response.status, response.readBytes())
        } catch (exception: Exception) {
            log.warn("Klarte ikke hente data fra '$proxyPath'. Feilmelding: ${exception.message}", exception)
            call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }

}


