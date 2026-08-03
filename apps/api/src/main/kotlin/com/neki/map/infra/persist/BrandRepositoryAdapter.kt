package com.neki.map.infra.persist

import com.neki.map.BrandRepository
import com.neki.map.infra.persist.jpa.JpaBrandRepository
import com.neki.map.models.Brand
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
