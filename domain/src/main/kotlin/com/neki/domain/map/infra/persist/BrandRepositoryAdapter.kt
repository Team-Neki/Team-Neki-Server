package com.neki.domain.map.infra.persist

import com.neki.domain.map.dto.BrandQuery
import com.neki.domain.map.infra.persist.jpa.JpaBrandRepository
import com.neki.domain.map.models.Brand
import com.neki.domain.map.repository.BrandRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

/**
 * fileName       : BrandRepositoryAdapter
 * author         : darren
 * date           : 2026. 1. 16. 11:32
 * description    :
 */
@Repository
class BrandRepositoryAdapter(private val jpaRepository: JpaBrandRepository) : BrandRepository {
    override fun save(brand: Brand): Brand = jpaRepository.save(brand)

    override fun findById(id: Long): Brand? = jpaRepository.findByIdOrNull(id)

    override fun existsByName(name: String): Boolean = jpaRepository.existsByName(name)

    override fun existsByCode(code: String): Boolean = jpaRepository.existsByCode(code)

    override fun getBrand(code: String): Brand? = jpaRepository.findByCode(code)

    override fun findAll(): List<Brand> = jpaRepository.findAllByOrderByIdAsc()

    // 어드민 목록 조회 전용이라 apps:api 에서는 호출 경로가 없다. 필요해지면 QueryDSL 로 구현한다.
    override fun findAll(query: BrandQuery.GetBrands): List<Brand> =
        throw UnsupportedOperationException("어드민 전용 조회다. apps:admin 의 어댑터를 쓴다.")

    override fun count(query: BrandQuery.GetBrands): Long =
        throw UnsupportedOperationException("어드민 전용 조회다. apps:admin 의 어댑터를 쓴다.")

    override fun findByKeyword(query: BrandQuery.SearchBrands): List<Brand> =
        throw UnsupportedOperationException("어드민 전용 조회다. apps:admin 의 어댑터를 쓴다.")

    override fun countByKeyword(query: BrandQuery.SearchBrands): Long =
        throw UnsupportedOperationException("어드민 전용 조회다. apps:admin 의 어댑터를 쓴다.")
}
