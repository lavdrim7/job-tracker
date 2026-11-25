package com.jobtracker.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "applications")
class Application(
    @Column(nullable = false)
    var company: String,

    @Column(nullable = false)
    var position: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ApplicationStatus = ApplicationStatus.WISHLIST,

    var deadline: LocalDate? = null,

    @Column(name = "applied_date")
    var appliedDate: LocalDate? = null,

    @Column(name = "job_url")
    var jobUrl: String? = null,

    var notes: String? = null,
) {
    @Id
    @GeneratedValue
    var id: UUID? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.EPOCH

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.EPOCH

    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}
