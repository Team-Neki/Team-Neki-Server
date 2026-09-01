package com.neki.domain.map.repository

import com.neki.domain.map.dto.BrandQuery
import com.neki.domain.map.models.Brand

/**
 * fileName       : BrandRepositoryPort
 * author         : darren
 * date           : 2026. 1. 16. 11:31
 * description    :
 */
interface BrandRepository {

    fun save(brand: Brand): Brand

    fun findById(id: Long): Brand?

    fun existsByName(name: String): Boolean

    fun existsByCode(code: String): Boolean

    fun getBrand(code: String): Brand?

    fun findAll(): List<Brand>

    fun findAll(query: BrandQuery.GetBrands): List<Brand>

    fun count(query: BrandQuery.GetBrands): Long

    fun findByKeyword(query: BrandQuery.SearchBrands): List<Brand>

    fun countByKeyword(query: BrandQuery.SearchBrands): Long
}
