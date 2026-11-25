package com.jobtracker.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.jobtracker.TestcontainersConfiguration
import com.jobtracker.repository.ApplicationRepository
import com.jobtracker.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

/**
 * Hele kjeden (filter, kontroller, service, ekte PostgreSQL) med ekte tokens fra /api/auth.
 * Hver test bruker unike e-poster, så testene deler database uten å påvirke hverandre.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class SecurityIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val encoder: JwtEncoder,
    @Autowired private val users: UserRepository,
    @Autowired private val applications: ApplicationRepository,
) {
    private val password = "hemmelig-passord"
    private val body = """{"company":"Acme","position":"Utvikler","status":"APPLIED"}"""

    // ---------- hjelpere ----------

    private fun registerAndLogin(): Pair<UUID, String> {
        val email = "bruker-${UUID.randomUUID()}@example.com"
        val credentials = """{"email":"$email","password":"$password"}"""
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = credentials
        }.andExpect { status { isCreated() } }
        val login = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = credentials
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        return users.findByEmail(email)!!.id!! to objectMapper.readTree(login)["token"].asText()
    }

    private fun createApplication(token: String): UUID {
        val response = mockMvc.post("/api/applications") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return UUID.fromString(objectMapper.readTree(response)["id"].asText())
    }

    private fun tokenFor(userId: UUID, issued: Instant, expires: Instant?, encoder: JwtEncoder = this.encoder): String {
        val claims = JwtClaimsSet.builder().issuer("job-tracker").subject(userId.toString()).issuedAt(issued)
        if (expires != null) claims.expiresAt(expires)
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).tokenValue
    }

    private fun ResultActionsDsl.expectUnauthorized() = andExpect {
        status { isUnauthorized() }
        header { string(HttpHeaders.WWW_AUTHENTICATE, "Bearer") }
        jsonPath("$.status") { value(401) }
        jsonPath("$.message") { value("Mangler gyldig innlogging") }
        jsonPath("$.errors") { isEmpty() }
    }

    /** Kaller alle fem /api/applications-endepunkter med gitt Authorization-header (eller ingen). */
    private fun callAllEndpoints(authorization: String?): List<ResultActionsDsl> {
        val id = UUID.randomUUID()
        return listOf(
            mockMvc.get("/api/applications") { authorization?.let { header(HttpHeaders.AUTHORIZATION, it) } },
            mockMvc.get("/api/applications/$id") { authorization?.let { header(HttpHeaders.AUTHORIZATION, it) } },
            mockMvc.post("/api/applications") {
                authorization?.let { header(HttpHeaders.AUTHORIZATION, it) }
                contentType = MediaType.APPLICATION_JSON
                content = body
            },
            mockMvc.put("/api/applications/$id") {
                authorization?.let { header(HttpHeaders.AUTHORIZATION, it) }
                contentType = MediaType.APPLICATION_JSON
                content = body
            },
            mockMvc.delete("/api/applications/$id") { authorization?.let { header(HttpHeaders.AUTHORIZATION, it) } },
        )
    }

    // ---------- 401: manglende og ugyldige tokens ----------

    @Test
    fun `alle søknadsendepunkter gir 401 uten token`() {
        callAllEndpoints(null).forEach { it.expectUnauthorized() }
    }

    @Test
    fun `alle søknadsendepunkter gir 401 med ugyldig token`() {
        callAllEndpoints("Bearer dette.er-ikke.et-token").forEach { it.expectUnauthorized() }
        callAllEndpoints("Bearer ").forEach { it.expectUnauthorized() }
    }

    @Test
    fun `feil autentiseringsskjema gir 401`() {
        val (_, token) = registerAndLogin()
        callAllEndpoints("Basic $token").forEach { it.expectUnauthorized() }
        callAllEndpoints(token).forEach { it.expectUnauthorized() }
    }

    @Test
    fun `utløpt token gir 401 selv om det nettopp har utløpt`() {
        val (userId, _) = registerAndLogin()
        val now = Instant.now()
        val justExpired = tokenFor(userId, now.minusSeconds(60), now.minusSeconds(2))
        val longExpired = tokenFor(userId, now.minus(Duration.ofDays(2)), now.minus(Duration.ofDays(1)))

        callAllEndpoints("Bearer $justExpired").forEach { it.expectUnauthorized() }
        callAllEndpoints("Bearer $longExpired").forEach { it.expectUnauthorized() }
    }

    @Test
    fun `token signert med feil hemmelighet gir 401`() {
        val (userId, _) = registerAndLogin()
        val attackerKey = SecretKeySpec("angriper-hemmelighet-angriper-hemmelighet".toByteArray(), "HmacSHA256")
        val forged = tokenFor(
            userId, Instant.now(), Instant.now().plusSeconds(3600),
            NimbusJwtEncoder(ImmutableSecret(attackerKey)),
        )

        callAllEndpoints("Bearer $forged").forEach { it.expectUnauthorized() }
    }

    @Test
    fun `token uten utløpstid gir 401`() {
        val (userId, _) = registerAndLogin()
        val eternal = tokenFor(userId, Instant.now(), null)

        callAllEndpoints("Bearer $eternal").forEach { it.expectUnauthorized() }
    }

    @Test
    fun `manipulert token gir 401`() {
        val (_, token) = registerAndLogin()
        val (header, payload, signature) = token.split(".")
        // Bytt ut brukeren i payload uten å signere på nytt.
        val otherSubject = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString(
                String(java.util.Base64.getUrlDecoder().decode(payload))
                    .replace(Regex("\"sub\":\"[^\"]+\""), "\"sub\":\"${UUID.randomUUID()}\"")
                    .toByteArray(),
            )

        callAllEndpoints("Bearer $header.$otherSubject.$signature").forEach { it.expectUnauthorized() }
    }

    @Test
    fun `token med alg none gir 401`() {
        val (userId, _) = registerAndLogin()
        val b64 = java.util.Base64.getUrlEncoder().withoutPadding()
        val exp = Instant.now().plusSeconds(3600).epochSecond
        val unsigned = b64.encodeToString("""{"alg":"none"}""".toByteArray()) + "." +
            b64.encodeToString("""{"iss":"job-tracker","sub":"$userId","exp":$exp}""".toByteArray()) + "."

        callAllEndpoints("Bearer $unsigned").forEach { it.expectUnauthorized() }
    }

    @Test
    fun `helsesjekk og auth-endepunkter krever ikke token`() {
        mockMvc.get("/api/health").andExpect { status { isOk() } }
        mockMvc.get("/actuator/health").andExpect { status { isOk() } }
    }

    @Test
    fun `gyldig token gir tilgang`() {
        val (_, token) = registerAndLogin()

        mockMvc.get("/api/applications") { header(HttpHeaders.AUTHORIZATION, "Bearer $token") }
            .andExpect { status { isOk() } }
    }

    // ---------- Eierskap: bruker A kan ikke røre bruker Bs søknader ----------

    @Test
    fun `bruker A kan ikke lese bruker Bs søknad`() {
        val (_, tokenA) = registerAndLogin()
        val (_, tokenB) = registerAndLogin()
        val bsApplication = createApplication(tokenB)

        mockMvc.get("/api/applications/$bsApplication") { header(HttpHeaders.AUTHORIZATION, "Bearer $tokenA") }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.status") { value(404) }
                jsonPath("$.message") { value("Fant ingen søknad med id $bsApplication") }
            }
        mockMvc.get("/api/applications/$bsApplication") { header(HttpHeaders.AUTHORIZATION, "Bearer $tokenB") }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `svar for andres og ikke-eksisterende id er identiske`() {
        val (_, tokenA) = registerAndLogin()
        val (_, tokenB) = registerAndLogin()
        val bsApplication = createApplication(tokenB)
        val missing = UUID.randomUUID()

        fun responseFor(id: UUID) = mockMvc.get("/api/applications/$id") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $tokenA")
        }.andReturn().response

        val foreign = responseFor(bsApplication)
        val unknown = responseFor(missing)

        assertThat(foreign.status).isEqualTo(unknown.status).isEqualTo(404)
        assertThat(foreign.contentAsString.replace(bsApplication.toString(), "ID"))
            .isEqualTo(unknown.contentAsString.replace(missing.toString(), "ID"))
    }

    @Test
    fun `bruker A kan ikke endre bruker Bs søknad`() {
        val (_, tokenA) = registerAndLogin()
        val (_, tokenB) = registerAndLogin()
        val bsApplication = createApplication(tokenB)

        mockMvc.put("/api/applications/$bsApplication") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"Overtatt","position":"Hacket","status":"REJECTED"}"""
        }.andExpect { status { isNotFound() } }

        val unchanged = applications.findById(bsApplication).orElseThrow()
        assertThat(unchanged.company).isEqualTo("Acme")
        assertThat(unchanged.status.name).isEqualTo("APPLIED")
    }

    @Test
    fun `bruker A kan ikke slette bruker Bs søknad`() {
        val (_, tokenA) = registerAndLogin()
        val (_, tokenB) = registerAndLogin()
        val bsApplication = createApplication(tokenB)

        mockMvc.delete("/api/applications/$bsApplication") { header(HttpHeaders.AUTHORIZATION, "Bearer $tokenA") }
            .andExpect { status { isNotFound() } }

        assertThat(applications.existsById(bsApplication)).isTrue()
        mockMvc.delete("/api/applications/$bsApplication") { header(HttpHeaders.AUTHORIZATION, "Bearer $tokenB") }
            .andExpect { status { isNoContent() } }
        assertThat(applications.existsById(bsApplication)).isFalse()
    }

    @Test
    fun `lister inneholder bare egne søknader, også med filter og sortering`() {
        val (_, tokenA) = registerAndLogin()
        val (_, tokenB) = registerAndLogin()
        val own = createApplication(tokenA)
        val others = createApplication(tokenB)

        listOf("/api/applications", "/api/applications?status=APPLIED", "/api/applications?sort=deadline")
            .forEach { url ->
                val json = mockMvc.get(url) { header(HttpHeaders.AUTHORIZATION, "Bearer $tokenA") }
                    .andExpect { status { isOk() } }.andReturn().response.contentAsString
                val ids = objectMapper.readTree(json).map { it["id"].asText() }
                assertThat(ids).containsExactly(own.toString()).doesNotContain(others.toString())
            }
    }

    @Test
    fun `ny søknad får innlogget bruker som eier uansett hva klienten sender`() {
        val (userA, tokenA) = registerAndLogin()
        val (userB, _) = registerAndLogin()

        val response = mockMvc.post("/api/applications") {
            header(HttpHeaders.AUTHORIZATION, "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"Acme","position":"Utvikler","userId":"$userB"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString

        val id = UUID.fromString(objectMapper.readTree(response)["id"].asText())
        assertThat(applications.findById(id).orElseThrow().userId).isEqualTo(userA)
    }
}
