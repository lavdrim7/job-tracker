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
import java.util.UUID

class ApplicationServiceTest {

    private val repository = mockk<ApplicationRepository>()
    private val service = ApplicationService(repository)

    private val userId = UUID.randomUUID()
    private val id = UUID.randomUUID()

    @Test
    fun `create lagrer søknaden med trimmede navn`() {
        val saved = slot<Application>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        service.create(
            userId,
            ApplicationRequest(company = "  Acme ", position = " Utvikler  ", status = ApplicationStatus.APPLIED),
        )

        assertThat(saved.captured.company).isEqualTo("Acme")
        assertThat(saved.captured.position).isEqualTo("Utvikler")
        assertThat(saved.captured.status).isEqualTo(ApplicationStatus.APPLIED)
    }

    @Test
    fun `get returnerer søknaden når den finnes`() {
        val application = Application(userId = userId, company = "Acme", position = "Utvikler")
        every { repository.findByIdAndUserId(id, userId) } returns application

        assertThat(service.get(userId, id)).isSameAs(application)
    }

    @Test
    fun `get kaster ApplicationNotFoundException for ukjent id`() {
        every { repository.findByIdAndUserId(id, userId) } returns null

        assertThatThrownBy { service.get(userId, id) }
            .isInstanceOf(ApplicationNotFoundException::class.java)
            .hasMessageContaining(id.toString())
    }

    @Test
    fun `list uten status henter alle`() {
        every { repository.findAllByUserId(userId, any()) } returns emptyList()

        service.list(userId, null, ApplicationSort.CREATED)

        verify { repository.findAllByUserId(userId, Sort.by(Sort.Direction.DESC, "createdAt")) }
    }

    @Test
    fun `list med status filtrerer og sorterer nyeste først som standard`() {
        every { repository.findAllByUserIdAndStatus(userId, any(), any()) } returns emptyList()

        service.list(userId, ApplicationStatus.OFFER, ApplicationSort.CREATED)

        verify { repository.findAllByUserIdAndStatus(userId, ApplicationStatus.OFFER, Sort.by(Sort.Direction.DESC, "createdAt")) }
    }

    @Test
    fun `list sorterer på frist uten filter`() {
        every { repository.findAllByUserIdOrderByDeadline(userId) } returns emptyList()

        service.list(userId, null, ApplicationSort.DEADLINE)

        verify { repository.findAllByUserIdOrderByDeadline(userId) }
    }

    @Test
    fun `list sorterer på frist med statusfilter`() {
        every { repository.findAllByUserIdAndStatusOrderByDeadline(userId, ApplicationStatus.OFFER) } returns emptyList()

        service.list(userId, ApplicationStatus.OFFER, ApplicationSort.DEADLINE)

        verify { repository.findAllByUserIdAndStatusOrderByDeadline(userId, ApplicationStatus.OFFER) }
    }

    @Test
    fun `update overskriver alle felt`() {
        val application = Application(userId = userId, company = "Gammel", position = "Gammel stilling", notes = "gammelt notat")
        every { repository.findByIdAndUserId(id, userId) } returns application

        val updated = service.update(
            userId,
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
        every { repository.findByIdAndUserId(id, userId) } returns null

        assertThatThrownBy { service.update(userId, id, ApplicationRequest(company = "A", position = "B")) }
            .isInstanceOf(ApplicationNotFoundException::class.java)
    }

    @Test
    fun `delete sletter søknaden`() {
        val application = Application(userId = userId, company = "Acme", position = "Utvikler")
        every { repository.findByIdAndUserId(id, userId) } returns application
        every { repository.delete(application) } returns Unit

        service.delete(userId, id)

        verify { repository.delete(application) }
    }

    @Test
    fun `delete kaster ApplicationNotFoundException for ukjent id`() {
        every { repository.findByIdAndUserId(id, userId) } returns null

        assertThatThrownBy { service.delete(userId, id) }.isInstanceOf(ApplicationNotFoundException::class.java)
        verify(exactly = 0) { repository.delete(any()) }
    }

    @Test
    fun `create setter eieren til innlogget bruker`() {
        val saved = slot<Application>()
        every { repository.save(capture(saved)) } answers { saved.captured }

        service.create(userId, ApplicationRequest(company = "Acme", position = "Utvikler"))

        assertThat(saved.captured.userId).isEqualTo(userId)
    }

    @Test
    fun `get slår opp med eier, så andres søknader gir ApplicationNotFoundException`() {
        val otherUser = UUID.randomUUID()
        every { repository.findByIdAndUserId(id, otherUser) } returns null

        assertThatThrownBy { service.get(otherUser, id) }.isInstanceOf(ApplicationNotFoundException::class.java)
    }
}
