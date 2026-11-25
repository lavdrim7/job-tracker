package com.jobtracker.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.jobtracker.controller.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain

/** Svarer 401 i samme JSON-format som resten av API-et. Gjelder både manglende og ugyldig/utløpt token. */
class ApiAuthenticationEntryPoint(private val objectMapper: ObjectMapper) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        // Samme melding uansett årsak, så svaret ikke sier noe om hvorfor tokenet ble avvist.
        objectMapper.writeValue(
            response.outputStream,
            ErrorResponse(HttpStatus.UNAUTHORIZED.value(), "Mangler gyldig innlogging"),
        )
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity, objectMapper: ObjectMapper): SecurityFilterChain {
        val entryPoint = ApiAuthenticationEntryPoint(objectMapper)
        http
            // Stateless API med bearer-token i header (ingen cookies), så CSRF-beskyttelse gir ingenting her.
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/**", "/api/health", "/actuator/health", "/actuator/health/**").permitAll()
                it.anyRequest().authenticated()
            }
            .exceptionHandling { it.authenticationEntryPoint(entryPoint) }
            .oauth2ResourceServer {
                it.authenticationEntryPoint(entryPoint)
                it.jwt(Customizer.withDefaults())
            }
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
