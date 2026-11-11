package com.neki.api.map.application

import com.neki.core.annotation.UseCase
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.service.BrandService

/**
 * fileName       : GetVisibleBrandIdsUseCase
 * description    : 지도에 노출 중인(exposeToMap) 브랜드 ID 조회
 */
@UseCase
class GetVisibleBrandIdsUseCase(private val brandService: BrandService) {

    fun execute(query: MapQuery.GetVisibleBrandIds): List<Long> = brandService.getVisibleBrandIds(query)
}
