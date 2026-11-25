package com.jobtracker.service

import com.jobtracker.controller.ApplicationRequest
import com.jobtracker.domain.Application
import com.jobtracker.domain.ApplicationStatus
import com.jobtracker.repository.ApplicationRepository
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

class ApplicationNotFoundException(id: UUID) : RuntimeException("Fant ingen søknad med id $id")

enum class ApplicationSort { CREATED, DEADLINE }

/**
 * Alle metoder er avgrenset til [userId]. En søknad som tilhører en annen bruker oppfører seg
 * nøyaktig som en som ikke finnes (404), så id-er til andre brukere avsløres ikke.
 */
@Service
class ApplicationService(private val repository: ApplicationRepository) {

    @Transactional
    fun create(userId: UUID, request: ApplicationRequest): Application =
        repository.save(
            Application(
                userId = userId,
                company = request.company.trim(),
                position = request.position.trim(),
                status = request.status,
                deadline = request.deadline,
                appliedDate = request.appliedDate,
                jobUrl = request.jobUrl,
                notes = request.notes,
            ),
        )

    @Transactional(readOnly = true)
    fun get(userId: UUID, id: UUID): Application =
        repository.findByIdAndUserId(id, userId) ?: throw ApplicationNotFoundException(id)

    /** Sortering på frist er stigende med manglende frist sist. Standard er nyeste først. */
    @Transactional(readOnly = true)
    fun list(userId: UUID, status: ApplicationStatus?, sort: ApplicationSort): List<Application> = when (sort) {
        ApplicationSort.DEADLINE ->
            if (status == null) {
                repository.findAllByUserIdOrderByDeadline(userId)
            } else {
                repository.findAllByUserIdAndStatusOrderByDeadline(userId, status)
            }
        ApplicationSort.CREATED -> {
            val newestFirst = Sort.by(Sort.Direction.DESC, "createdAt")
            if (status == null) {
                repository.findAllByUserId(userId, newestFirst)
            } else {
                repository.findAllByUserIdAndStatus(userId, status, newestFirst)
            }
        }
    }

    @Transactional
    fun update(userId: UUID, id: UUID, request: ApplicationRequest): Application {
        val application = get(userId, id)
        application.company = request.company.trim()
        application.position = request.position.trim()
        application.status = request.status
        application.deadline = request.deadline
        application.appliedDate = request.appliedDate
        application.jobUrl = request.jobUrl
        application.notes = request.notes
        return application
    }

    @Transactional
    fun delete(userId: UUID, id: UUID) {
        repository.delete(get(userId, id))
    }
}
