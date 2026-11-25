package com.jobtracker.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.jobtracker.TestcontainersConfiguration
import com.jobtracker.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Duration
import java.util.UUID

/** Registrering og innlogging mot ekte database og ekte BCrypt/JWT. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration::class)
class AuthIntegrationTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
    @Autowired private val users: UserRepository,
    @Autowired private val decoder: JwtDecoder,
) {
    private val password = "hemmelig-passord"

    private fun uniqueEmail() = "bruker-${UUID.randomUUID()}@example.com"

    private fun post(path: String, json: String) = mockMvc.post(path) {
        contentType = MediaType.APPLICATION_JSON
        content = json
    }

    private fun credentials(email: String, password: String) = """{"email":"$email","password":"$password"}"""

    @Test
    fun `registrering lagrer bruker med BCrypt-hash og returnerer aldri passord eller hash`() {
        val email = uniqueEmail()

        val response = post("/api/auth/register", credentials(email, password))
            .andExpect {
                status { isCreated() }
                jsonPath("$.email") { value(email) }
                jsonPath("$.id") { exists() }
                jsonPath("$.createdAt") { exists() }
            }.andReturn().response.contentAsString

        assertThat(response).doesNotContain(password).doesNotContainIgnoringCase("hash")
        val stored = users.findByEmail(email)!!
        assertThat(stored.passwordHash).startsWith("\$2").isNotEqualTo(password)
    }

    @Test
    fun `e-post er uavhengig av store og små bokstaver og mellomrom`() {
        val email = uniqueEmail()
        post("/api/auth/register", credentials(email.uppercase(), password)).andExpect { status { isCreated() } }

        post("/api/auth/register", credentials(email, password)).andExpect {
            status { isConflict() }
            jsonPath("$.message") { value("E-postadressen er allerede registrert") }
        }
        post("/api/auth/login", credentials(email.uppercase(), password)).andExpect { status { isOk() } }
    }

    @Test
    fun `ugyldig registrering gir 400 og lagrer ingenting`() {
        val email = uniqueEmail()

        post("/api/auth/register", credentials(email, "for-kort")).andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0]") { value("password må være mellom 10 og 72 tegn") }
        }
        post("/api/auth/register", credentials("ikke-en-epost", password)).andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0]") { value("email må være en gyldig e-postadresse") }
        }
        assertThat(users.existsByEmail(email)).isFalse()
    }

    @Test
    fun `innlogging gir gyldig JWT med utløpstid som kan brukes mot API-et`() {
        val email = uniqueEmail()
        post("/api/auth/register", credentials(email, password)).andExpect { status { isCreated() } }

        val json = post("/api/auth/login", credentials(email, password)).andExpect {
            status { isOk() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.expiresIn") { value(3600) }
        }.andReturn().response.contentAsString
        val token = objectMapper.readTree(json)["token"].asText()

        val jwt = decoder.decode(token)
        assertThat(jwt.subject).isEqualTo(users.findByEmail(email)!!.id.toString())
        assertThat(Duration.between(jwt.issuedAt, jwt.expiresAt)).isEqualTo(Duration.ofHours(1))
        mockMvc.get("/api/applications") { header(HttpHeaders.AUTHORIZATION, "Bearer $token") }
            .andExpect { status { isOk() } }
    }

    @Test
    fun `feil passord og ukjent e-post gir byte-for-byte lik 401`() {
        val email = uniqueEmail()
        post("/api/auth/register", credentials(email, password)).andExpect { status { isCreated() } }

        val wrongPassword = post("/api/auth/login", credentials(email, "feil-passord-123"))
            .andExpect { status { isUnauthorized() } }.andReturn().response
        val unknownEmail = post("/api/auth/login", credentials(uniqueEmail(), password))
            .andExpect { status { isUnauthorized() } }.andReturn().response

        assertThat(wrongPassword.contentAsString).isEqualTo(unknownEmail.contentAsString)
        assertThat(wrongPassword.contentAsString)
            .isEqualTo("""{"status":401,"message":"Ugyldig e-post eller passord","errors":[]}""")
        assertThat(wrongPassword.getHeaders(HttpHeaders.WWW_AUTHENTICATE))
            .isEqualTo(unknownEmail.getHeaders(HttpHeaders.WWW_AUTHENTICATE))
    }

    @Test
    fun `innlogging uten e-post eller passord gir 400`() {
        post("/api/auth/login", """{"email":"","password":""}""").andExpect { status { isBadRequest() } }
    }
}
