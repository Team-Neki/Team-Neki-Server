package com.neki.map.application.dto

import com.neki.map.models.Brand
import com.neki.map.models.MediaMetadata

/**
 * fileName       : BrandAssembler
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 브랜드에 로고 media를 붙여 응답 항목으로 조립한다.
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
}
