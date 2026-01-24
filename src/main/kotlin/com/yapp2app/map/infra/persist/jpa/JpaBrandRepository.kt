package com.yapp2app.map.infra.persist.jpa

import com.yapp2app.map.domain.entity.Brand
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaBrandRepository
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : Brand JPA Repository
 */
interface JpaBrandRepository : JpaRepository<Brand, Long> {

    fun findByCode(code: String): Brand?
}
