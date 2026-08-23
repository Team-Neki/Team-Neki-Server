package com.neki.api.map.application

import com.neki.api.map.application.dto.BrandAssembler
import com.neki.api.map.application.dto.MapResult
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.map.client.MediaClient
import com.neki.domain.map.dto.MapQuery
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.MediaMetadata
import com.neki.domain.map.service.BrandService
import com.neki.domain.map.service.MapService

/**
 * fileName       : GetPolygonBrandUseCase
 * author         : darren
 * date           : 2026. 8. 23.
 * description    : 다각형 영역 내에 포토부스가 존재하는 브랜드만 조회
 */
@UseCase
class GetPolygonBrandUseCase(
    private val mapService: MapService,
    private val brandService: BrandService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(query: MapQuery.GetPolygonBrand): List<MapResult.GetBrand> {
        // 위치 애그리거트에서 얻은 brandIds 를 브랜드 애그리거트로 넘기는 순서는 유스케이스가 결정한다
        val sortedBrands: List<Brand> = transactionRunner.readOnly {
            val brandIds: List<Long> = mapService.getBrandIdsInPolygon(query)

            if (brandIds.isEmpty()) return@readOnly emptyList()

            brandService.getBrandsByIds(query, brandIds)
        }

        if (sortedBrands.isEmpty()) return emptyList()

        // 다른 도메인 호출은 트랜잭션 밖에서
        val medias: List<MediaMetadata> = mediaClient.getMediaMetadata(sortedBrands.mapNotNull { it.mediaId })

        return BrandAssembler.toBrands(sortedBrands, medias)
    }
}
