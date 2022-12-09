package no.nav.tms.min.side.proxy.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import no.nav.tms.min.side.proxy.common.AccessToken

object HttpClientBuilder {

    fun build(jsonSerializer: KotlinxSerializer = KotlinxSerializer(jsonConfig())): HttpClient {
        return HttpClient(Apache) {
            install(JsonFeature) {
                serializer = jsonSerializer
            }
            install(HttpTimeout)
        }
    }

}


suspend inline fun <reified T> HttpClient.get(url: String, accessToken: AccessToken): T = withContext(Dispatchers.IO) {
    request {
        url(url)
        method = HttpMethod.Get
        header(HttpHeaders.Authorization, "Bearer ${accessToken.value}")
    }
}

suspend inline fun <reified T> HttpClient.post(url: String, content: JsonElement, accessToken: AccessToken): T =
    withContext(Dispatchers.IO) {
        request {
            url(url)
            method = HttpMethod.Post
            header(HttpHeaders.Authorization, "Bearer ${accessToken.value}")
            contentType(ContentType.Application.Json)
            body = content
        }
    }


