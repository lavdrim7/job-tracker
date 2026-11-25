package com.jobtracker.controller

import com.jobtracker.domain.ApplicationStatus
import com.jobtracker.service.ApplicationService
import com.jobtracker.service.ApplicationSort
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.util.UUID

@RestController
@RequestMapping("/api/applications")
class ApplicationController(private val service: ApplicationService) {

    @PostMapping
    fun create(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: ApplicationRequest,
    ): ResponseEntity<ApplicationResponse> {
        val created = service.create(jwt.userId(), request).toResponse()
        val location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id).toUri()
        return ResponseEntity.created(location).body(created)
    }

    @GetMapping("/{id}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID): ApplicationResponse =
        service.get(jwt.userId(), id).toResponse()

    /** `?status=APPLIED` filtrerer, `?sort=deadline` sorterer på frist (stigende, uten frist sist). */
    @GetMapping
    fun list(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) status: ApplicationStatus?,
        @RequestParam(required = false, defaultValue = "created") sort: String,
    ): List<ApplicationResponse> {
        val sorting = when (sort.lowercase()) {
            "deadline" -> ApplicationSort.DEADLINE
            "created" -> ApplicationSort.CREATED
            else -> throw IllegalArgumentException("Ugyldig sort '$sort'. Gyldige verdier: deadline, created")
        }
        return service.list(jwt.userId(), status, sorting).map { it.toResponse() }
    }

    @PutMapping("/{id}")
    fun update(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable id: UUID,
        @Valid @RequestBody request: ApplicationRequest,
    ): ApplicationResponse = service.update(jwt.userId(), id, request).toResponse()

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: UUID) = service.delete(jwt.userId(), id)
}

/** Innlogget bruker er token-subjektet. */
private fun Jwt.userId(): UUID = UUID.fromString(subject)
