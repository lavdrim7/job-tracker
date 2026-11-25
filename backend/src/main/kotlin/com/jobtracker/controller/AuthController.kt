package com.jobtracker.controller

import com.jobtracker.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val service: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UserResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.register(request).toResponse())

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse {
        val issued = service.login(request)
        return LoginResponse(token = issued.token, expiresIn = issued.expiresInSeconds)
    }
}
