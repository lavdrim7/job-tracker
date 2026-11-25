package com.jobtracker.controller

import com.jobtracker.domain.User
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

data class RegisterRequest(
    // Krever punktum i domenet, ellers godtar @Email "bruker@host".
    @field:NotBlank(message = "email må fylles ut")
    @field:Email(regexp = ".+@.+\\..+", message = "email må være en gyldig e-postadresse")
    @field:Size(max = 255, message = "email kan ikke være lengre enn 255 tegn")
    val email: String,

    // BCrypt bruker bare de første 72 byte, så lengre passord ville gitt falsk trygghet.
    @field:NotBlank(message = "password må fylles ut")
    @field:Size(min = 10, max = 72, message = "password må være mellom 10 og 72 tegn")
    val password: String,
) {
    override fun toString() = "RegisterRequest(email=$email, password=***)"
}

/** Ingen lengdekrav her: feil passord skal gi samme svar uansett hvordan det ser ut. */
data class LoginRequest(
    @field:NotBlank(message = "email må fylles ut")
    val email: String,

    @field:NotBlank(message = "password må fylles ut")
    val password: String,
) {
    override fun toString() = "LoginRequest(email=$email, password=***)"
}

/** Inneholder aldri passord eller hash. */
data class UserResponse(val id: UUID, val email: String, val createdAt: Instant)

data class LoginResponse(val token: String, val tokenType: String = "Bearer", val expiresIn: Long)

fun User.toResponse() = UserResponse(
    id = checkNotNull(id) { "Brukeren er ikke lagret" },
    email = email,
    createdAt = createdAt,
)
