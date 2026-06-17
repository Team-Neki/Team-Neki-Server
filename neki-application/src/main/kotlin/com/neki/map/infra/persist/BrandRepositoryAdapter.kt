package com.neki.map.infra.persist

import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.domain.entity.Brand
import com.neki.map.infra.persist.jpa.JpaBrandRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : BrandRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 16. 11:32
 * description    :
 */
@Repository
class BrandRepositoryAdapter(private val jpaRepository: JpaBrandRepository) : BrandRepositoryPort {

    override fun getBrand(code: String): Brand? = jpaRepository.findByCode(code)

    override fun findAll(): List<Brand> = jpaRepository.findAllByOrderByIdAsc()
}
