package com.jobtracker

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForEntity
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus

/** Starter hele applikasjonen mot en ekte PostgreSQL i Testcontainers. */
@Import(TestcontainersConfiguration::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class JobTrackerApplicationTests(@Autowired private val restTemplate: TestRestTemplate) {

    @Test
    fun `applikasjonen starter`() {
    }

    @Test
    fun `actuator rapporterer at applikasjon og database er oppe`() {
        val response = restTemplate.getForEntity<String>("/actuator/health")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).contains("\"status\":\"UP\"")
    }
}
