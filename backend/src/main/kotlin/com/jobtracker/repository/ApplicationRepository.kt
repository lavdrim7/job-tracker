package com.jobtracker.repository

import com.jobtracker.domain.Application
import com.jobtracker.domain.ApplicationStatus
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

/** Alle oppslag er avgrenset til eieren (userId), så en bruker aldri kan nå andres søknader. */
interface ApplicationRepository : JpaRepository<Application, UUID> {
    fun findByIdAndUserId(id: UUID, userId: UUID): Application?

    fun findAllByUserId(userId: UUID, sort: Sort): List<Application>

    fun findAllByUserIdAndStatus(userId: UUID, status: ApplicationStatus, sort: Sort): List<Application>

    // Spring Data støtter ikke NULLS LAST via Sort, så frist-sorteringen er eksplisitt JPQL.
    @Query("select a from Application a where a.userId = :userId order by a.deadline asc nulls last, a.createdAt desc")
    fun findAllByUserIdOrderByDeadline(userId: UUID): List<Application>

    @Query(
        "select a from Application a where a.userId = :userId and a.status = :status " +
            "order by a.deadline asc nulls last, a.createdAt desc",
    )
    fun findAllByUserIdAndStatusOrderByDeadline(userId: UUID, status: ApplicationStatus): List<Application>
}
