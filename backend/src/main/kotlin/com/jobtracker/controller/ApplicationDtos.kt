package com.jobtracker.controller

import com.jobtracker.domain.Application
import com.jobtracker.domain.ApplicationStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Brukes både for opprettelse og oppdatering (PUT erstatter hele ressursen). */
data class ApplicationRequest(
    @field:NotBlank(message = "company må fylles ut")
    @field:Size(max = 255, message = "company kan ikke være lengre enn 255 tegn")
    val company: String,

    @field:NotBlank(message = "position må fylles ut")
    @field:Size(max = 255, message = "position kan ikke være lengre enn 255 tegn")
    val position: String,

    val status: ApplicationStatus = ApplicationStatus.WISHLIST,
    val deadline: LocalDate? = null,
    val appliedDate: LocalDate? = null,

    @field:Size(max = 2048, message = "jobUrl kan ikke være lengre enn 2048 tegn")
    val jobUrl: String? = null,
    val notes: String? = null,
)

data class ApplicationResponse(
    val id: UUID,
    val company: String,
    val position: String,
    val status: ApplicationStatus,
    val deadline: LocalDate?,
    val appliedDate: LocalDate?,
    val jobUrl: String?,
    val notes: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun Application.toResponse() = ApplicationResponse(
    id = checkNotNull(id) { "Entiteten er ikke lagret" },
    company = company,
    position = position,
    status = status,
    deadline = deadline,
    appliedDate = appliedDate,
    jobUrl = jobUrl,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
