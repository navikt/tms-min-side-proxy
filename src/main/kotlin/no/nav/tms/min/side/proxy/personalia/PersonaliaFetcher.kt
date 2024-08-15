package no.nav.tms.min.side.proxy.personalia

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nav.tms.token.support.tokendings.exchange.TokendingsService
import no.nav.tms.token.support.tokenx.validation.user.TokenXUser
import java.time.Duration

class PersonaliaFetcher(
    private val client: HttpClient,
    private val pdlUrl: String,
    private val pdlClientId: String,
    private val pdlBehandlingsnummer: String,
    private val tokendingsService: TokendingsService
) {

    private val cache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofMinutes(5))
        .build<String, String>()

    private val log = KotlinLogging.logger {}
    private val securelog = KotlinLogging.logger("secureLog")

    fun getNavn(user: TokenXUser): String {
        return cache.get(user.ident) {
            fetchNavn(user)
        }
    }

    private fun fetchNavn(user: TokenXUser): String = runBlocking(Dispatchers.IO) {
        tokendingsService.exchangeToken(user.tokenString, pdlClientId)
            .let { token -> queryForNavn(user.ident, token) }
            .let { response -> checkForErrors(response) }
            .hentPerson.fullnavn
    }

    private suspend fun queryForNavn(ident: String, token: String): HentNavnResponse {
        val response = client.post {
            url(pdlUrl)
            header(HttpHeaders.Authorization, "Bearer $token")
            header("Behandlingsnummer", pdlBehandlingsnummer)
            header("Tema", "GEN")
            contentType(ContentType.Application.Json)
            setBody(HentNavn(ident))
        }

        if (!response.status.isSuccess()) {
            throw HentNavnException("Fikk http-feil fra PDL")
        }

        return try {
            response.body()
        } catch (e: Exception) {
            securelog.error(e) { "Klarer ikke tolke svar fra PDL." }
            throw HentNavnException("Klarte ikke tolke svar fra PDL", e)
        }
    }

    private fun checkForErrors(response: HentNavnResponse): HentNavnResponse.HentNavnData {

        response.errors?.let { errors ->
            if (errors.isNotEmpty()) {
                log.warn { "Feil i GraphQL-responsen: $errors" }
                throw HentNavnException("Feil i responsen under henting av navn")
            }
        }

        return response.data?: throw HentNavnException("Ingen data i graphql-svar.")
    }
}

