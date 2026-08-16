package com.neki.admin.config

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

/**
 * fileName       : QueryDslConfig
 * author         : koo
 * date           : 2026. 8. 9.
 * description    : admin 은 modules:postgres 를 의존하지 않아 QueryDSL·JPA Auditing 을 직접 구성한다
 */
@Configuration
@EnableJpaAuditing
class QueryDslConfig(
    @PersistenceContext
    val entityManager: EntityManager,
) {

    @Bean
    fun queryFactory(): JPAQueryFactory = JPAQueryFactory(entityManager)
}
