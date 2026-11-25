package com.jobtracker.repository

import com.jobtracker.TestcontainersConfiguration
import com.jobtracker.domain.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration::class)
class UserRepositoryTest(@Autowired private val repository: UserRepository) {

    @Test
    fun `lagrer bruker og finner den på e-post`() {
        val saved = repository.saveAndFlush(User("ola@example.com", "hash"))

        val found = repository.findByEmail("ola@example.com")

        assertThat(found?.id).isEqualTo(saved.id)
        assertThat(found?.passwordHash).isEqualTo("hash")
        assertThat(found?.createdAt).isAfter(java.time.Instant.EPOCH)
        assertThat(repository.existsByEmail("ola@example.com")).isTrue()
        assertThat(repository.findByEmail("ukjent@example.com")).isNull()
    }

    @Test
    fun `e-post må være unik`() {
        repository.saveAndFlush(User("dobbel@example.com", "hash"))

        assertThatThrownBy { repository.saveAndFlush(User("dobbel@example.com", "annen-hash")) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `databasen nekter e-post med store bokstaver`() {
        assertThatThrownBy { repository.saveAndFlush(User("Stor@Example.com", "hash")) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
