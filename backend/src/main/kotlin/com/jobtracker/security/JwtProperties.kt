package com.jobtracker.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Leses fra `app.jwt.*`. `secret` har ingen standardverdi (se application.yml), og ugyldige verdier
 * gjør at applikasjonen nekter å starte.
 */
@ConfigurationProperties("app.jwt")
data class JwtProperties(
    val secret: String,
    val expiration: Duration = Duration.ofHours(1),
    val issuer: String = "job-tracker",
) {
    init {
        require(secret.isNotBlank()) { "JWT_SECRET må settes" }
        require(secret.toByteArray(Charsets.UTF_8).size >= MIN_SECRET_BYTES) {
            "JWT_SECRET må være minst $MIN_SECRET_BYTES tegn (256 bit) for HS256"
        }
        require(!expiration.isZero && !expiration.isNegative) { "JWT_EXPIRATION må være positiv" }
    }

    // Hemmeligheten skal aldri havne i logger via toString().
    override fun toString() = "JwtProperties(secret=***, expiration=$expiration, issuer=$issuer)"

    companion object {
        const val MIN_SECRET_BYTES = 32
    }
}
