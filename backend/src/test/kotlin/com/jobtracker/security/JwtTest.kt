package com.jobtracker.security

import com.jobtracker.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtException
import java.time.Duration
import java.time.Instant
import java.util.UUID

class JwtTest {

    private val secret = "x".repeat(32)
    private val properties = JwtProperties(secret = secret, expiration = Duration.ofMinutes(30))
    private val config = JwtConfig()
    private val tokens = TokenService(config.jwtEncoder(properties), properties)
    private val decoder: JwtDecoder = config.jwtDecoder(properties)

    @Test
    fun `utstedt token har bruker-id som subject og utløper etter konfigurert tid`() {
        val userId = UUID.randomUUID()
        val issued = tokens.issue(User("a@example.com", "hash").also { it.id = userId })

        val jwt = decoder.decode(issued.token)

        assertThat(jwt.subject).isEqualTo(userId.toString())
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("job-tracker")
        assertThat(Duration.between(jwt.issuedAt, jwt.expiresAt)).isEqualTo(Duration.ofMinutes(30))
        assertThat(issued.expiresInSeconds).isEqualTo(1800)
        assertThat(jwt.claims.keys).doesNotContain("email", "password")
    }

    @Test
    fun `token signert med annen hemmelighet avvises`() {
        val other = JwtProperties(secret = "y".repeat(32))
        val forged = TokenService(JwtConfig().jwtEncoder(other), other)
            .issue(User("a@example.com", "hash").also { it.id = UUID.randomUUID() })

        assertThatThrownBy { decoder.decode(forged.token) }.isInstanceOf(JwtException::class.java)
    }

    @Test
    fun `utløpt token avvises uten klokketoleranse`() {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder().issuer("job-tracker").subject(UUID.randomUUID().toString())
            .issuedAt(now.minusSeconds(60)).expiresAt(now.minusSeconds(2)).build()
        val expired = config.jwtEncoder(properties)
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).tokenValue

        assertThatThrownBy { decoder.decode(expired) }.isInstanceOf(JwtException::class.java)
    }

    @Test
    fun `token fra annen utsteder avvises`() {
        val foreign = properties.copy(issuer = "noen-andre")
        val token = TokenService(config.jwtEncoder(foreign), foreign)
            .issue(User("a@example.com", "hash").also { it.id = UUID.randomUUID() })

        assertThatThrownBy { decoder.decode(token.token) }.isInstanceOf(JwtException::class.java)
    }

    @Test
    fun `rotete tokens avvises`() {
        assertThatThrownBy { decoder.decode("ikke.et.token") }.isInstanceOf(JwtException::class.java)
        assertThatThrownBy { decoder.decode("") }.isInstanceOf(JwtException::class.java)
    }

    @Test
    fun `for kort eller tom hemmelighet gir feil`() {
        assertThatThrownBy { JwtProperties(secret = "for-kort") }.hasMessageContaining("minst 32")
        assertThatThrownBy { JwtProperties(secret = "   ") }.hasMessageContaining("JWT_SECRET")
        assertThatThrownBy { JwtProperties(secret = secret, expiration = Duration.ZERO) }
            .hasMessageContaining("positiv")
    }

    @Test
    fun `toString lekker ikke hemmeligheten`() {
        assertThat(properties.toString()).doesNotContain(secret)
    }

    @Test
    fun `applikasjonskonteksten starter ikke uten app jwt secret`() {
        ApplicationContextRunner()
            .withUserConfiguration(JwtConfig::class.java)
            .run { context -> assertThat(context).hasFailed() }
    }

    @Test
    fun `applikasjonskonteksten starter ikke med for kort hemmelighet`() {
        ApplicationContextRunner()
            .withUserConfiguration(JwtConfig::class.java)
            .withPropertyValues("app.jwt.secret=for-kort")
            .run { context -> assertThat(context).hasFailed() }
    }

    @Test
    fun `applikasjonskonteksten starter med gyldig hemmelighet`() {
        ApplicationContextRunner()
            .withUserConfiguration(JwtConfig::class.java)
            .withPropertyValues("app.jwt.secret=$secret", "app.jwt.expiration=PT15M")
            .run { context ->
                assertThat(context).hasNotFailed()
                assertThat(context.getBean(JwtProperties::class.java).expiration).isEqualTo(Duration.ofMinutes(15))
            }
    }
}
