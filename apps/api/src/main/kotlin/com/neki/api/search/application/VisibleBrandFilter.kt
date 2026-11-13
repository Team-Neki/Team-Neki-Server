package com.neki.api.search.application

import com.neki.core.annotation.UseCase
import com.neki.domain.search.client.MapClient

@UseCase
class VisibleBrandFilter(private val mapClient: MapClient) {

    fun allVisibleIds(): List<Long> = mapClient.findVisibleBrandIds(null)

    fun filter(brandIds: List<Long>): List<Long> {
        if (brandIds.isEmpty()) return emptyList()

        val visibleBrandIds = mapClient.findVisibleBrandIds(brandIds).toSet()
        return brandIds.filter { it in visibleBrandIds }
    }
}
