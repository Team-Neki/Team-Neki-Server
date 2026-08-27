package com.neki.api.map.application.dto

import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.MediaMetadata

/**
 * fileName       : BrandAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 브랜드에 로고 media·집계값을 붙여 응답 항목으로 조립한다.
 */
object BrandAssembler {

    /**
     * 로고 media가 아직 없는 브랜드는 storageKey 없이 반환한다.
     */
    fun toBrands(brands: List<Brand>, medias: List<MediaMetadata>): List<MapResult.GetBrand> {
        val mediaByMediaId: Map<Long, MediaMetadata> = medias.associateBy { it.mediaId }

        return brands.map { brand ->
            MapResult.GetBrand(
                id = brand.id!!,
                name = brand.name,
                code = brand.code,
                storageKey = brand.mediaId?.let { mediaByMediaId[it]?.storageKey },
            )
        }
    }

    /**
     * 영역 내 브랜드 목록에 브랜드별 포토부스 개수를 붙여 필터 항목으로 조립한다.
     * brands 는 boothCounts 의 key 로 조회한 결과이므로 개수가 반드시 존재한다.
     */
    fun toPolygonFilter(brands: List<Brand>, boothCounts: Map<Long, Long>): MapResult.PolygonFilter {
        val brandFilter: List<MapResult.BrandFilter> = brands.map { brand ->
            MapResult.BrandFilter(
                id = brand.id!!,
                name = brand.name,
                code = brand.code,
                count = boothCounts.getValue(brand.id!!),
            )
        }

        return MapResult.PolygonFilter(brandFilter = brandFilter)
    }
}
