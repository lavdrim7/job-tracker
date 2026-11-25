package com.jobtracker.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    /** Alltid trimmet og med små bokstaver. */
    @Column(nullable = false, unique = true)
    var email: String,

    /** BCrypt-hash. Skal aldri eksponeres i API-et. */
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
) {
    @Id
    @GeneratedValue
    var id: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH

    @PrePersist
    fun onCreate() {
        createdAt = Instant.now()
    }
}
