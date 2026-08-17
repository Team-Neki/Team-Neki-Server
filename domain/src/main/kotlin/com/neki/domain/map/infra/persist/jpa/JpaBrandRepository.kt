package com.neki.domain.map.infra.persist.jpa

import com.neki.domain.map.models.Brand
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaBrandRepository
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : Brand JPA Repository
 */
interface JpaBrandRepository : JpaRepository<Brand, Long> {
    fun findAllByOrderByIdAsc(): List<Brand>

    fun findByCode(code: String): Brand?
}
