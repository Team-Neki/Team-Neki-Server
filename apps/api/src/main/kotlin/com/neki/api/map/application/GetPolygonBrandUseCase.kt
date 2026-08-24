package com.neki.api.map.application

import com.neki.api.map.application.dto.MapResult
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.Brand
import com.neki.domain.map.service.BrandService
import com.neki.domain.map.service.MapService

/**
 * fileName       : GetPolygonBrandUseCase
 * author         : darren
 * date           : 2026. 8. 23.
 * description    : 다각형 영역 내에 포토부스가 존재하는 브랜드만 조회
 *
 * 브랜드 로고는 브랜드 전체 조회(GET /api/photo-booths/brand)에서 이미 내려주므로
 * 이 API 는 media 도메인을 호출하지 않는다. 클라이언트는 id 로 기존 값을 재사용한다.
 */
@UseCase
class GetPolygonBrandUseCase(
    private val mapService: MapService,
    private val brandService: BrandService,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: MapQuery.GetPolygonBrand): List<MapResult.GetPolygonBrand> {
        // 위치 애그리거트에서 얻은 brandIds 를 브랜드 애그리거트로 넘기는 순서는 유스케이스가 결정한다
        val (sortedBrands: List<Brand>, boothCounts: Map<Long, Long>) = transactionRunner.readOnly {
            val counts: Map<Long, Long> = mapService.getBrandBoothCountsInPolygon(query)

            if (counts.isEmpty()) return@readOnly emptyList<Brand>() to counts

            brandService.getBrandsByIds(query, counts.keys.toList()) to counts
        }

        // 브랜드는 counts 의 key 로 조회했으므로 개수가 반드시 존재한다
        return sortedBrands.map {
            MapResult.GetPolygonBrand(
                id = it.id!!,
                name = it.name,
                code = it.code,
                boothCount = boothCounts.getValue(it.id!!),
            )
        }
    }
}
