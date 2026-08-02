package com.neki.config.postgres

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * fileName       : QueryDslConfig
 * author         : koo
 * date           : 2026. 1. 12. 오후 10:02
 * description    :
 */
@Configuration
class QueryDslConfig(
    @PersistenceContext
    val entityManager: EntityManager,
) {

    @Bean
    fun queryFactory(): JPAQueryFactory = JPAQueryFactory(entityManager)
}
