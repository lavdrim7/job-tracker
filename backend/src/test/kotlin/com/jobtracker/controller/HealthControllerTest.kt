package com.jobtracker.controller

import com.jobtracker.security.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(HealthController::class)
@Import(SecurityConfig::class)
class HealthControllerTest(@Autowired private val mockMvc: MockMvc) {

    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    @Test
    fun `GET api health svarer med status UP uten token`() {
        mockMvc.get("/api/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
        }
    }
}
