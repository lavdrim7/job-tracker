package com.jobtracker.repository

import com.jobtracker.domain.Application
import com.jobtracker.domain.ApplicationStatus
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ApplicationRepository : JpaRepository<Application, UUID> {
    fun findAllByStatus(status: ApplicationStatus, sort: Sort): List<Application>

    // Spring Data støtter ikke NULLS LAST via Sort, så frist-sorteringen er eksplisitt JPQL.
    @Query("select a from Application a order by a.deadline asc nulls last, a.createdAt desc")
    fun findAllOrderByDeadline(): List<Application>

    @Query("select a from Application a where a.status = :status order by a.deadline asc nulls last, a.createdAt desc")
    fun findAllByStatusOrderByDeadline(status: ApplicationStatus): List<Application>
}
