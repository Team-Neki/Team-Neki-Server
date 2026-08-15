package com.neki.domain.map.infra.persist

import com.neki.domain.map.infra.persist.jpa.JpaBrandRepository
import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandRepository
import org.springframework.stereotype.Repository

/**
 * fileName       : BrandRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 16. 11:32
 * description    :
 */
@Repository
class BrandRepositoryAdapter(private val jpaRepository: JpaBrandRepository) : BrandRepository {

    override fun getBrand(code: String): Brand? = jpaRepository.findByCode(code)

    override fun findAll(): List<Brand> = jpaRepository.findAllByOrderByIdAsc()
}
