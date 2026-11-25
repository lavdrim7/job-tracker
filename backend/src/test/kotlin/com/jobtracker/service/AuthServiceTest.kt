package com.jobtracker.service

import com.jobtracker.controller.LoginRequest
import com.jobtracker.controller.RegisterRequest
import com.jobtracker.domain.User
import com.jobtracker.repository.UserRepository
import com.jobtracker.security.IssuedToken
import com.jobtracker.security.TokenService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.UUID

class AuthServiceTest {

    private val users = mockk<UserRepository>()
    private val tokens = mockk<TokenService>()
    private val encoder = BCryptPasswordEncoder(4) // lav kostnad holder testene raske
    private val service = AuthService(users, encoder, tokens)

    private val storedUser = User("a@example.com", encoder.encode("riktig-passord-1")).also { it.id = UUID.randomUUID() }

    @Test
    fun `register hasher passordet og normaliserer e-posten`() {
        val saved = slot<User>()
        every { users.existsByEmail("ola@example.com") } returns false
        every { users.saveAndFlush(capture(saved)) } answers { saved.captured }

        service.register(RegisterRequest("  Ola@Example.COM ", "hemmelig-passord"))

        assertThat(saved.captured.email).isEqualTo("ola@example.com")
        assertThat(saved.captured.passwordHash).isNotEqualTo("hemmelig-passord").startsWith("\$2")
        assertThat(encoder.matches("hemmelig-passord", saved.captured.passwordHash)).isTrue()
    }

    @Test
    fun `register avviser e-post som finnes fra før`() {
        every { users.existsByEmail("a@example.com") } returns true

        assertThatThrownBy { service.register(RegisterRequest("A@example.com", "hemmelig-passord")) }
            .isInstanceOf(EmailAlreadyRegisteredException::class.java)
        verify(exactly = 0) { users.saveAndFlush(any()) }
    }

    @Test
    fun `register oversetter databasens unike constraint ved samtidig registrering`() {
        every { users.existsByEmail(any()) } returns false
        every { users.saveAndFlush(any()) } throws DataIntegrityViolationException("duplicate key")

        assertThatThrownBy { service.register(RegisterRequest("a@example.com", "hemmelig-passord")) }
            .isInstanceOf(EmailAlreadyRegisteredException::class.java)
    }

    @Test
    fun `login gir token ved riktig e-post og passord`() {
        val issued = IssuedToken("token", 3600)
        every { users.findByEmail("a@example.com") } returns storedUser
        every { tokens.issue(storedUser) } returns issued

        assertThat(service.login(LoginRequest(" A@Example.com", "riktig-passord-1"))).isEqualTo(issued)
    }

    @Test
    fun `feil passord og ukjent e-post gir identisk feil`() {
        every { users.findByEmail("a@example.com") } returns storedUser
        every { users.findByEmail("ukjent@example.com") } returns null

        val wrongPassword = catchLoginFailure(LoginRequest("a@example.com", "feil-passord-123"))
        val unknownEmail = catchLoginFailure(LoginRequest("ukjent@example.com", "riktig-passord-1"))

        assertThat(wrongPassword).isInstanceOf(InvalidCredentialsException::class.java)
        assertThat(unknownEmail).isInstanceOf(InvalidCredentialsException::class.java)
        assertThat(wrongPassword.message).isEqualTo(unknownEmail.message)
        verify(exactly = 0) { tokens.issue(any()) }
    }

    private fun catchLoginFailure(request: LoginRequest): Throwable {
        var failure: Throwable? = null
        try {
            service.login(request)
        } catch (e: Throwable) {
            failure = e
        }
        return checkNotNull(failure) { "Forventet at innloggingen feilet" }
    }
}
