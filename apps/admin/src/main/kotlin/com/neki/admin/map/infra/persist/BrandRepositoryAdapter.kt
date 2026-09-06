package com.neki.admin.map.infra.persist

import com.neki.admin.map.infra.persist.jpa.BrandQueryRepository
import com.neki.admin.map.infra.persist.jpa.JpaBrandRepository
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.map.dto.BrandQuery
import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * fileName       : BrandRepositoryAdapter
 * author         : koo
 * date           : 2026. 8. 9. 오후 11:10
 * description    :
 */
@Repository
class BrandRepositoryAdapter(
    private val jpaRepository: JpaBrandRepository,
    private val queryRepository: BrandQueryRepository,
) : BrandRepository {

    override fun save(brand: Brand): Brand = jpaRepository.save(brand)

    override fun findById(id: Long): Brand =
        jpaRepository.findByIdOrNull(id) ?: throw BusinessException(ResultCode.NOT_FOUND)

    override fun existsByName(name: String): Boolean = jpaRepository.existsByName(name)

    override fun existsByCode(code: String): Boolean = jpaRepository.existsByCode(code)

    override fun getBrand(code: String): Brand? = jpaRepository.findByCode(code)

    override fun findAll(): List<Brand> = jpaRepository.findAllByOrderByIdAsc()

    override fun findAll(query: BrandQuery.GetBrands): List<Brand> = queryRepository.findAll(query)

    override fun count(query: BrandQuery.GetBrands): Long = queryRepository.count(query)

    override fun findByKeyword(query: BrandQuery.SearchBrands): List<Brand> = queryRepository.findByKeyword(query)

    override fun countByKeyword(query: BrandQuery.SearchBrands): Long = queryRepository.countByKeyword(query)
}
