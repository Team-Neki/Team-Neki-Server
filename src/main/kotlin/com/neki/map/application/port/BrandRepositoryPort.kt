package com.neki.map.application.port

import com.neki.map.domain.entity.Brand

/**
 * fileName       : BrandRepositoryPort
 * author         : darren
 * date           : 2026. 1. 16. 11:31
 * description    :
 */
interface BrandRepositoryPort {

    fun getBrand(code: String): Brand?

    fun findAll(): List<Brand>
}
