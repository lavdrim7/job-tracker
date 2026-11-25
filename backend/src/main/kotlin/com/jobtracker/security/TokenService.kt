package com.jobtracker.security

import com.jobtracker.domain.User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant

data class IssuedToken(val token: String, val expiresInSeconds: Long)

@Service
class TokenService(private val encoder: JwtEncoder, private val properties: JwtProperties) {

    /** Subjektet er bruker-id. Ingen personopplysninger legges i tokenet. */
    fun issue(user: User): IssuedToken {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(checkNotNull(user.id) { "Brukeren er ikke lagret" }.toString())
            .issuedAt(now)
            .expiresAt(now.plus(properties.expiration))
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
        return IssuedToken(token, properties.expiration.seconds)
    }
}
