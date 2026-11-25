package com.jobtracker.controller

import com.jobtracker.domain.User
import com.jobtracker.security.IssuedToken
import com.jobtracker.security.SecurityConfig
import com.jobtracker.service.AuthService
import com.jobtracker.service.EmailAlreadyRegisteredException
import com.jobtracker.service.InvalidCredentialsException
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.util.UUID

@WebMvcTest(AuthController::class)
@Import(SecurityConfig::class)
class AuthControllerTest(@Autowired private val mockMvc: MockMvc) {

    @MockkBean
    private lateinit var service: AuthService

    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    private fun register(body: String) = mockMvc.post("/api/auth/register") {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun login(body: String) = mockMvc.post("/api/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    @Test
    fun `register svarer 201 uten passord eller hash i svaret`() {
        every { service.register(any()) } returns
            User("ola@example.com", "\$2a\$10\$hemmelig-hash").also { it.id = UUID.randomUUID() }

        register("""{"email":"ola@example.com","password":"hemmelig-passord"}""").andExpect {
            status { isCreated() }
            jsonPath("$.email") { value("ola@example.com") }
            jsonPath("$.id") { exists() }
            jsonPath("$.password") { doesNotExist() }
            jsonPath("$.passwordHash") { doesNotExist() }
            jsonPath("$.password_hash") { doesNotExist() }
            content { string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hemmelig"))) }
        }
    }

    @Test
    fun `register med ugyldig e-postformat gir 400`() {
        listOf("ola", "ola@", "@example.com", "ola@host", "ola example@x.no", "").forEach { email ->
            register("""{"email":"$email","password":"hemmelig-passord"}""").andExpect {
                status { isBadRequest() }
                jsonPath("$.status") { value(400) }
                jsonPath("$.errors") { isNotEmpty() }
            }
        }
        verify(exactly = 0) { service.register(any()) }
    }

    @Test
    fun `register med passord under 10 tegn gir 400`() {
        register("""{"email":"ola@example.com","password":"123456789"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0]") { value("password må være mellom 10 og 72 tegn") }
        }
        verify(exactly = 0) { service.register(any()) }
    }

    @Test
    fun `register godtar passord på nøyaktig 10 tegn`() {
        every { service.register(any()) } returns User("ola@example.com", "hash").also { it.id = UUID.randomUUID() }

        register("""{"email":"ola@example.com","password":"1234567890"}""").andExpect { status { isCreated() } }
    }

    @Test
    fun `register med passord over 72 tegn gir 400`() {
        register("""{"email":"ola@example.com","password":"${"a".repeat(73)}"}""")
            .andExpect { status { isBadRequest() } }
    }

    @Test
    fun `register uten felt gir 400`() {
        register("{}").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `register med e-post som finnes gir 409 i vanlig feilformat`() {
        every { service.register(any()) } throws EmailAlreadyRegisteredException()

        register("""{"email":"ola@example.com","password":"hemmelig-passord"}""").andExpect {
            status { isConflict() }
            jsonPath("$.status") { value(409) }
            jsonPath("$.message") { value("E-postadressen er allerede registrert") }
        }
    }

    @Test
    fun `login svarer med token og utløpstid`() {
        every { service.login(any()) } returns IssuedToken("jwt-verdi", 3600)

        login("""{"email":"ola@example.com","password":"hemmelig-passord"}""").andExpect {
            status { isOk() }
            jsonPath("$.token") { value("jwt-verdi") }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.expiresIn") { value(3600) }
        }
    }

    @Test
    fun `login med feil opplysninger gir 401 i vanlig feilformat`() {
        every { service.login(any()) } throws InvalidCredentialsException()

        login("""{"email":"ola@example.com","password":"feil"}""").andExpect {
            status { isUnauthorized() }
            jsonPath("$.status") { value(401) }
            jsonPath("$.message") { value("Ugyldig e-post eller passord") }
        }
    }

    @Test
    fun `login uten passord gir 400`() {
        login("""{"email":"ola@example.com"}""").andExpect { status { isBadRequest() } }
    }
}
