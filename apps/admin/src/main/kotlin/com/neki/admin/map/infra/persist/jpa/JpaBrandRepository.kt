package com.neki.admin.map.infra.persist.jpa

import com.neki.domain.map.models.Brand
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * fileName       : JpaBrandRepository
 * author         : koo
 * date           : 2026. 8. 9.
 * description    : Brand JPA Repository
 */
interface JpaBrandRepository : JpaRepository<Brand, Long> {
    fun findAllByOrderByIdAsc(): List<Brand>

    @Query("select b.id from Brand b where b.exposeToMap = true")
    fun findVisibleIds(): List<Long>

    @Query("select b.id from Brand b where b.exposeToMap = true and b.id in :ids")
    fun findVisibleIdsByIdIn(@Param("ids") ids: List<Long>): List<Long>

    fun findByCode(code: String): Brand?

    fun existsByName(name: String): Boolean

    fun existsByCode(code: String): Boolean
}
