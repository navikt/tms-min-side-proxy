package no.nav.tms.min.side.proxy.dittnav


import io.ktor.client.statement.readBytes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.util.pipeline.PipelineContext
import no.nav.tms.min.side.proxy.config.accessToken
import no.nav.tms.min.side.proxy.config.jsonConfig
import no.nav.tms.min.side.proxy.config.proxyPath
import org.slf4j.LoggerFactory

fun Route.dittnavApi(consumer: DittnavConsumer) {

    val log = LoggerFactory.getLogger(DittnavConsumer::class.java)

    get("/dittnav/{proxyPath...}") {
            val response = consumer.getContent(accessToken, proxyPath)
            call.respond(response.status, response.readBytes())
    }

    //TODO: skriv test
    post("/dittnav/{proxyPath}") {
        val proxyPath = call.parameters["proxyPath"]

        try {
            val content = jsonConfig().parseToJsonElement(call.receiveText())
            val response = consumer.postContent(accessToken, content, proxyPath)
            call.respond(response.status)
        } catch (exception: Exception) {
            log.warn("Klarte ikke poste data. Feilmelding: ${exception.message}", exception)
            call.respond(HttpStatusCode.ServiceUnavailable)
        }
    }
}

