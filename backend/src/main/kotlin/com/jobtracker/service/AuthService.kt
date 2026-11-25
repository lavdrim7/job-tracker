package com.jobtracker.service

import com.jobtracker.controller.LoginRequest
import com.jobtracker.controller.RegisterRequest
import com.jobtracker.domain.User
import com.jobtracker.repository.UserRepository
import com.jobtracker.security.IssuedToken
import com.jobtracker.security.TokenService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class EmailAlreadyRegisteredException : RuntimeException("E-postadressen er allerede registrert")

/** Én felles melding for ukjent e-post og feil passord, så svaret ikke avslører hvilke e-poster som finnes. */
class InvalidCredentialsException : RuntimeException("Ugyldig e-post eller passord")

@Service
class AuthService(
    private val users: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val tokens: TokenService,
) {
    // Brukes når e-posten ikke finnes, slik at svartiden ikke skiller ukjent e-post fra feil passord.
    private val dummyHash: String by lazy { passwordEncoder.encode("ikke-et-ekte-passord") }

    @Transactional
    fun register(request: RegisterRequest): User {
        val email = normalize(request.email)
        if (users.existsByEmail(email)) throw EmailAlreadyRegisteredException()
        try {
            return users.saveAndFlush(User(email = email, passwordHash = passwordEncoder.encode(request.password)))
        } catch (e: DataIntegrityViolationException) {
            // To samtidige registreringer med samme e-post: databasens unike constraint avgjør.
            throw EmailAlreadyRegisteredException()
        }
    }

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): IssuedToken {
        val user = users.findByEmail(normalize(request.email))
        val passwordMatches = passwordEncoder.matches(request.password, user?.passwordHash ?: dummyHash)
        if (user == null || !passwordMatches) throw InvalidCredentialsException()
        return tokens.issue(user)
    }

    private fun normalize(email: String) = email.trim().lowercase()
}
