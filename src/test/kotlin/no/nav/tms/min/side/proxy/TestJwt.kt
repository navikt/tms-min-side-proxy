package no.nav.tms.min.side.proxy

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.Principal
import io.ktor.server.auth.principal
import no.nav.security.token.support.core.jwt.JwtToken
import java.security.Key
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.ZonedDateTime
import java.util.Base64
import java.util.Date

class JwtStub(private val issuer: String = "test issuer") {

    private val privateKey: RSAPrivateKey
    private val publicKey: RSAPublicKey

    init {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(512)

        val keyPair = keyPairGenerator.genKeyPair()
        privateKey = keyPair.private as RSAPrivateKey
        publicKey = keyPair.public as RSAPublicKey
    }

    fun createTokenFor(pid: String, audience: String = "", authLevel: String = "Level4"): String {
        val algorithm = Algorithm.RSA256(publicKey, privateKey)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("pid", pid)
            .withClaim("acr",authLevel)
            .sign(algorithm)
    }

    fun stubbedJwkProvider(): StubbedJwkProvider {
        return StubbedJwkProvider(publicKey)
    }

    class StubbedJwkProvider(private val publicKey: RSAPublicKey) : JwkProvider {
        override fun get(keyId: String?): Jwk {
            return Jwk(
                keyId, "RSA", "RS256", "sig", listOf(), null, null, null,
                mapOf(
                    "e" to String(Base64.getEncoder().encode(publicKey.publicExponent.toByteArray())),
                    "n" to String(Base64.getEncoder().encode(publicKey.modulus.toByteArray()))
                )
            )
        }
    }
}

object TestUser {

    private val key: Key = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256)

    fun createAuthenticatedUser(): AuthenticatedUser {
        val ident = "12345"
        val innloggingsnivaa = 4
        return createAuthenticatedUser(ident, innloggingsnivaa)
    }

    fun createAuthenticatedUser(ident: String, innloggingsnivaa: Int): AuthenticatedUser {
        val inTwoMinutes = ZonedDateTime.now().plusMinutes(2)
        val jws = Jwts.builder()
            .setSubject(ident)
            .addClaims(mutableMapOf(Pair("acr", "Level$innloggingsnivaa")) as Map<String, Any>?)
            .setExpiration(Date.from(inTwoMinutes.toInstant()))
            .signWith(key).compact()
        val token = JwtToken(jws)
        return AuthenticatedUser(ident, innloggingsnivaa, token.tokenAsString)
    }
}

data class AuthenticatedUser (
    val ident: String,
    val loginLevel: Int,
    val token: String,
)

object AuthenticatedUserFactory {

    private fun createNewAuthenticatedUser(principal: PrincipalWithTokenString): AuthenticatedUser {

        val ident: String = principal.payload.getClaim("pid").asString()
        val loginLevel =
            extractLoginLevel(principal.payload)
        return AuthenticatedUser(ident, loginLevel, principal.accessToken)
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