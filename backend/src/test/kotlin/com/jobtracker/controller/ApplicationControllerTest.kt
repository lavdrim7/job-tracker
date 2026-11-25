package com.jobtracker.controller

import com.jobtracker.domain.Application
import com.jobtracker.domain.ApplicationStatus
import com.jobtracker.service.ApplicationNotFoundException
import com.jobtracker.service.ApplicationService
import com.jobtracker.service.ApplicationSort
import com.jobtracker.security.SecurityConfig
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(ApplicationController::class)
@Import(SecurityConfig::class)
class ApplicationControllerTest(@Autowired private val mockMvc: MockMvc) {

    @MockkBean
    private lateinit var service: ApplicationService

    // Tokenvalidering testes mot ekte dekoder i SecurityIntegrationTest. Her brukes en ferdig autentisert JWT.
    @MockkBean
    private lateinit var jwtDecoder: JwtDecoder

    private val id = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val auth = jwt().jwt { it.subject(userId.toString()) }

    private fun entity(company: String = "Acme") =
        Application(
            userId = userId,
            company = company,
            position = "Utvikler",
            status = ApplicationStatus.APPLIED,
            deadline = LocalDate.of(2026, 12, 1),
        ).also { it.id = id }

    @Test
    fun `POST oppretter søknad og svarer 201 med Location`() {
        every { service.create(userId, any()) } returns entity()

        mockMvc.post("/api/applications") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"Acme","position":"Utvikler","status":"APPLIED","deadline":"2026-12-01"}"""
        }.andExpect {
            status { isCreated() }
            header { string("Location", org.hamcrest.Matchers.endsWith("/api/applications/$id")) }
            jsonPath("$.id") { value(id.toString()) }
            jsonPath("$.company") { value("Acme") }
            jsonPath("$.status") { value("APPLIED") }
            jsonPath("$.deadline") { value("2026-12-01") }
        }
    }

    @Test
    fun `POST uten status gir WISHLIST som standard`() {
        every { service.create(userId, any()) } returns entity()

        mockMvc.post("/api/applications") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"Acme","position":"Utvikler"}"""
        }.andExpect { status { isCreated() } }

        verify { service.create(userId, match { it.status == ApplicationStatus.WISHLIST }) }
    }

    @Test
    fun `POST med tom company og position gir 400 med lesbare feilmeldinger`() {
        mockMvc.post("/api/applications") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"  ","position":""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.status") { value(400) }
            jsonPath("$.errors[0]") { value("company må fylles ut") }
            jsonPath("$.errors[1]") { value("position må fylles ut") }
        }
        verify(exactly = 0) { service.create(userId, any()) }
    }

    @Test
    fun `POST uten company gir 400`() {
        mockMvc.post("/api/applications") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"position":"Utvikler"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { exists() }
        }
    }

    @Test
    fun `POST med ugyldig status gir 400`() {
        mockMvc.post("/api/applications") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"Acme","position":"Utvikler","status":"HIRED"}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `POST med ugyldig dato gir 400`() {
        mockMvc.post("/api/applications") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"Acme","position":"Utvikler","deadline":"i morgen"}"""
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `GET én søknad svarer 200`() {
        every { service.get(userId, id) } returns entity()

        mockMvc.get("/api/applications/$id") { with(auth) }.andExpect {
            status { isOk() }
            jsonPath("$.company") { value("Acme") }
            jsonPath("$.position") { value("Utvikler") }
        }
    }

    @Test
    fun `GET ukjent id gir 404 med melding`() {
        every { service.get(userId, id) } throws ApplicationNotFoundException(id)

        mockMvc.get("/api/applications/$id") { with(auth) }.andExpect {
            status { isNotFound() }
            jsonPath("$.status") { value(404) }
            jsonPath("$.message") { value("Fant ingen søknad med id $id") }
        }
    }

    @Test
    fun `GET med id som ikke er UUID gir 400`() {
        mockMvc.get("/api/applications/abc") { with(auth) }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `GET liste bruker standard sortering uten filter`() {
        every { service.list(userId, null, ApplicationSort.CREATED) } returns listOf(entity("A"), entity("B"))

        mockMvc.get("/api/applications") { with(auth) }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].company") { value("A") }
        }
    }

    @Test
    fun `GET liste sender status og frist-sortering videre til service`() {
        every { service.list(userId, ApplicationStatus.APPLIED, ApplicationSort.DEADLINE) } returns listOf(entity())

        mockMvc.get("/api/applications?status=APPLIED&sort=deadline") { with(auth) }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(1) }
        }
    }

    @Test
    fun `GET liste med ugyldig status gir 400`() {
        mockMvc.get("/api/applications?status=HIRED") { with(auth) }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Ugyldig verdi 'HIRED' for parameteren 'status'") }
        }
    }

    @Test
    fun `GET liste med ugyldig sort gir 400`() {
        mockMvc.get("/api/applications?sort=company") { with(auth) }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `PUT oppdaterer søknad`() {
        every { service.update(userId, id, any()) } returns entity("Ny")

        mockMvc.put("/api/applications/$id") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"Ny","position":"Utvikler","status":"APPLIED"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.company") { value("Ny") }
        }
    }

    @Test
    fun `PUT ukjent id gir 404`() {
        every { service.update(userId, id, any()) } throws ApplicationNotFoundException(id)

        mockMvc.put("/api/applications/$id") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"Ny","position":"Utvikler"}"""
        }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `PUT med ugyldig innhold gir 400`() {
        mockMvc.put("/api/applications/$id") {
            with(auth)
            contentType = MediaType.APPLICATION_JSON
            content = """{"company":"","position":"Utvikler"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.errors[0]") { value("company må fylles ut") }
        }
    }

    @Test
    fun `DELETE svarer 204`() {
        every { service.delete(userId, id) } returns Unit

        mockMvc.delete("/api/applications/$id") { with(auth) }.andExpect { status { isNoContent() } }
    }

    @Test
    fun `DELETE ukjent id gir 404`() {
        every { service.delete(userId, id) } throws ApplicationNotFoundException(id)

        mockMvc.delete("/api/applications/$id") { with(auth) }.andExpect { status { isNotFound() } }
    }

    @Test
    fun `uten token gir 401 i vanlig feilformat og service kalles ikke`() {
        mockMvc.get("/api/applications").andExpect {
            status { isUnauthorized() }
            jsonPath("$.status") { value(401) }
            jsonPath("$.message") { exists() }
        }
        verify(exactly = 0) { service.list(any(), any(), any()) }
    }
}
