package com.neki.domain.map.repository

import com.neki.domain.map.models.Brand

/**
 * fileName       : BrandRepositoryPort
 * author         : darren
 * date           : 2026. 1. 16. 11:31
 * description    :
 */
interface BrandRepository {

    fun getBrand(code: String): Brand?

    fun findAll(): List<Brand>
}
