package com.jobtracker.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Enkel sjekk på at API-et svarer. Helsesjekk som også dekker databasen ligger på /actuator/health.
 */
@RestController
@RequestMapping("/api/health")
class HealthController {

    @GetMapping
    fun health() = HealthResponse(status = "UP")
}

data class HealthResponse(val status: String)
