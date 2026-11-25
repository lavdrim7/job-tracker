package com.jobtracker.service

import com.jobtracker.controller.ApplicationRequest
import com.jobtracker.domain.Application
import com.jobtracker.domain.ApplicationStatus
import com.jobtracker.repository.ApplicationRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Sort
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class ApplicationServiceTest {

    private val repository = mockk<ApplicationRepository>()
    private val service = ApplicationService(repository)

    private val id = UUID.randomUUID()

    @Test
    fun `create lagrer søknaden med trimmede navn`() {
        val saved = slot<Application>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        service.create(
            ApplicationRequest(company = "  Acme ", position = " Utvikler  ", status = ApplicationStatus.APPLIED),
        )

        assertThat(saved.captured.company).isEqualTo("Acme")
        assertThat(saved.captured.position).isEqualTo("Utvikler")
        assertThat(saved.captured.status).isEqualTo(ApplicationStatus.APPLIED)
    }

    @Test
    fun `get returnerer søknaden når den finnes`() {
        val application = Application(company = "Acme", position = "Utvikler")
        every { repository.findById(id) } returns Optional.of(application)

        assertThat(service.get(id)).isSameAs(application)
    }

    @Test
    fun `get kaster ApplicationNotFoundException for ukjent id`() {
        every { repository.findById(id) } returns Optional.empty()

        assertThatThrownBy { service.get(id) }
            .isInstanceOf(ApplicationNotFoundException::class.java)
            .hasMessageContaining(id.toString())
    }

    @Test
    fun `list uten status henter alle`() {
        every { repository.findAll(any<Sort>()) } returns emptyList()

        service.list(null, ApplicationSort.CREATED)

        verify { repository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")) }
    }

    @Test
    fun `list med status filtrerer og sorterer nyeste først som standard`() {
        every { repository.findAllByStatus(any(), any()) } returns emptyList()

        service.list(ApplicationStatus.OFFER, ApplicationSort.CREATED)

        verify { repository.findAllByStatus(ApplicationStatus.OFFER, Sort.by(Sort.Direction.DESC, "createdAt")) }
    }

    @Test
    fun `list sorterer på frist uten filter`() {
        every { repository.findAllOrderByDeadline() } returns emptyList()

        service.list(null, ApplicationSort.DEADLINE)

        verify { repository.findAllOrderByDeadline() }
    }

    @Test
    fun `list sorterer på frist med statusfilter`() {
        every { repository.findAllByStatusOrderByDeadline(ApplicationStatus.OFFER) } returns emptyList()

        service.list(ApplicationStatus.OFFER, ApplicationSort.DEADLINE)

        verify { repository.findAllByStatusOrderByDeadline(ApplicationStatus.OFFER) }
    }

    @Test
    fun `update overskriver alle felt`() {
        val application = Application(company = "Gammel", position = "Gammel stilling", notes = "gammelt notat")
        every { repository.findById(id) } returns Optional.of(application)

        val updated = service.update(
            id,
            ApplicationRequest(
                company = "Ny",
                position = "Ny stilling",
                status = ApplicationStatus.INTERVIEW,
                deadline = LocalDate.of(2026, 12, 1),
            ),
        )

        assertThat(updated.company).isEqualTo("Ny")
        assertThat(updated.position).isEqualTo("Ny stilling")
        assertThat(updated.status).isEqualTo(ApplicationStatus.INTERVIEW)
        assertThat(updated.deadline).isEqualTo(LocalDate.of(2026, 12, 1))
        assertThat(updated.notes).isNull()
    }

    @Test
    fun `update kaster ApplicationNotFoundException for ukjent id`() {
        every { repository.findById(id) } returns Optional.empty()

        assertThatThrownBy { service.update(id, ApplicationRequest(company = "A", position = "B")) }
            .isInstanceOf(ApplicationNotFoundException::class.java)
    }

    @Test
    fun `delete sletter søknaden`() {
        val application = Application(company = "Acme", position = "Utvikler")
        every { repository.findById(id) } returns Optional.of(application)
        every { repository.delete(application) } returns Unit

        service.delete(id)

        verify { repository.delete(application) }
    }

    @Test
    fun `delete kaster ApplicationNotFoundException for ukjent id`() {
        every { repository.findById(id) } returns Optional.empty()

        assertThatThrownBy { service.delete(id) }.isInstanceOf(ApplicationNotFoundException::class.java)
        verify(exactly = 0) { repository.delete(any()) }
    }
}
