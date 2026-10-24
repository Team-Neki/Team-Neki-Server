package com.neki.api.search.application

import com.neki.core.annotation.UseCase
import com.neki.domain.map.repository.BrandRepository

@UseCase
class VisibleBrandFilter(private val brandRepository: BrandRepository) {

    fun filter(brandIds: List<Long>): List<Long> {
        if (brandIds.isEmpty()) return emptyList()

        val visibleBrandIds = brandRepository.findAll().filter { it.exposeToMap }.mapNotNull { it.id }.toSet()
        return brandIds.filter { it in visibleBrandIds }
    }
}
