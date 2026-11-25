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

@Service
class ApplicationService(private val repository: ApplicationRepository) {

    @Transactional
    fun create(request: ApplicationRequest): Application =
        repository.save(
            Application(
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
    fun get(id: UUID): Application =
        repository.findById(id).orElseThrow { ApplicationNotFoundException(id) }

    /** Sortering på frist er stigende med manglende frist sist. Standard er nyeste først. */
    @Transactional(readOnly = true)
    fun list(status: ApplicationStatus?, sort: ApplicationSort): List<Application> = when (sort) {
        ApplicationSort.DEADLINE ->
            if (status == null) repository.findAllOrderByDeadline() else repository.findAllByStatusOrderByDeadline(status)
        ApplicationSort.CREATED -> {
            val newestFirst = Sort.by(Sort.Direction.DESC, "createdAt")
            if (status == null) repository.findAll(newestFirst) else repository.findAllByStatus(status, newestFirst)
        }
    }

    @Transactional
    fun update(id: UUID, request: ApplicationRequest): Application {
        val application = get(id)
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
    fun delete(id: UUID) {
        repository.delete(get(id))
    }
}
