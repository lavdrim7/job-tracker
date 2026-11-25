package com.jobtracker.repository

import com.jobtracker.TestcontainersConfiguration
import com.jobtracker.domain.Application
import com.jobtracker.domain.ApplicationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.Sort
import java.time.LocalDate

/** Kjører mot ekte PostgreSQL (Testcontainers) med Flyway-migrasjonene, ikke en in-memory-database. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class)
class ApplicationRepositoryTest(
    @Autowired private val repository: ApplicationRepository,
    @Autowired private val em: TestEntityManager,
) {

    private fun app(
        company: String,
        status: ApplicationStatus = ApplicationStatus.WISHLIST,
        deadline: LocalDate? = null,
    ) = Application(company = company, position = "Utvikler", status = status, deadline = deadline)

    @Test
    fun `lagrer og leser en søknad med alle felt`() {
        val saved = repository.saveAndFlush(
            Application(
                company = "Acme",
                position = "Backend-utvikler",
                status = ApplicationStatus.APPLIED,
                deadline = LocalDate.of(2026, 12, 1),
                appliedDate = LocalDate.of(2026, 11, 20),
                jobUrl = "https://acme.example/jobs/1",
                notes = "Ring på fredag",
            ),
        )
        em.clear()

        val found = repository.findById(saved.id!!).orElseThrow()

        assertThat(found.company).isEqualTo("Acme")
        assertThat(found.status).isEqualTo(ApplicationStatus.APPLIED)
        assertThat(found.deadline).isEqualTo(LocalDate.of(2026, 12, 1))
        assertThat(found.appliedDate).isEqualTo(LocalDate.of(2026, 11, 20))
        assertThat(found.jobUrl).isEqualTo("https://acme.example/jobs/1")
        assertThat(found.notes).isEqualTo("Ring på fredag")
        assertThat(found.createdAt).isAfter(java.time.Instant.EPOCH)
    }

    @Test
    fun `valgfrie felt kan være null`() {
        val saved = repository.saveAndFlush(app("Acme"))
        em.clear()

        val found = repository.findById(saved.id!!).orElseThrow()

        assertThat(found.deadline).isNull()
        assertThat(found.appliedDate).isNull()
        assertThat(found.jobUrl).isNull()
        assertThat(found.notes).isNull()
    }

    @Test
    fun `updatedAt endres ved oppdatering mens createdAt består`() {
        val saved = repository.saveAndFlush(app("Acme"))
        val created = saved.createdAt
        val firstUpdate = saved.updatedAt

        saved.status = ApplicationStatus.INTERVIEW
        repository.saveAndFlush(saved)

        assertThat(saved.createdAt).isEqualTo(created)
        assertThat(saved.updatedAt).isAfterOrEqualTo(firstUpdate)
    }

    @Test
    fun `filtrerer på status`() {
        repository.save(app("A", ApplicationStatus.APPLIED))
        repository.save(app("B", ApplicationStatus.REJECTED))
        repository.save(app("C", ApplicationStatus.APPLIED))
        repository.flush()

        val result = repository.findAllByStatus(ApplicationStatus.APPLIED, Sort.by("company"))

        assertThat(result.map { it.company }).containsExactly("A", "C")
    }

    @Test
    fun `sorterer på frist stigende med manglende frist sist`() {
        repository.save(app("Uten frist"))
        repository.save(app("Sen", deadline = LocalDate.of(2026, 12, 24)))
        repository.save(app("Tidlig", deadline = LocalDate.of(2026, 12, 1)))
        repository.flush()

        val result = repository.findAllOrderByDeadline()

        assertThat(result.map { it.company }).containsExactly("Tidlig", "Sen", "Uten frist")
    }

    @Test
    fun `filtrerer på status og sorterer på frist`() {
        repository.save(app("Uten frist", ApplicationStatus.APPLIED))
        repository.save(app("Sen", ApplicationStatus.APPLIED, LocalDate.of(2026, 12, 24)))
        repository.save(app("Annen status", ApplicationStatus.REJECTED, LocalDate.of(2026, 11, 1)))
        repository.save(app("Tidlig", ApplicationStatus.APPLIED, LocalDate.of(2026, 12, 1)))
        repository.flush()

        val result = repository.findAllByStatusOrderByDeadline(ApplicationStatus.APPLIED)

        assertThat(result.map { it.company }).containsExactly("Tidlig", "Sen", "Uten frist")
    }
}
