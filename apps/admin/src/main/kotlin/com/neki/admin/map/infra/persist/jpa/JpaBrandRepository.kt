package com.neki.admin.map.infra.persist.jpa

import com.neki.domain.map.models.Brand
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaBrandRepository
 * author         : koo
 * date           : 2026. 8. 9.
 * description    : Brand JPA Repository
 */
interface JpaBrandRepository : JpaRepository<Brand, Long> {
    fun findAllByIsDeletedFalseOrderByIdAsc(): List<Brand>

    fun findByCode(code: String): Brand?
}
