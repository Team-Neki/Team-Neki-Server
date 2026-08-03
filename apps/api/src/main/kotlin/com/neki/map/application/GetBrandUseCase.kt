package com.neki.map.application

import com.neki.common.annotation.UseCase
import com.neki.map.MediaClient
import com.neki.map.application.dto.BrandAssembler
import com.neki.map.application.dto.MapResult
import com.neki.map.dto.MapQuery
import com.neki.map.models.Brand
import com.neki.map.models.MediaMetadata
import com.neki.map.service.BrandService

/**
 * fileName       : GetBrandUseCase
 * author         : darren
 * date           : 2026. 1. 21. 14:41
 * description    : Brand 조회
 */
@UseCase
class GetBrandUseCase(private val brandService: BrandService, private val mediaClient: MediaClient) {

    fun execute(query: MapQuery.GetBrand): List<MapResult.GetBrand> {
        val sortedBrands: List<Brand> = brandService.getBrand(query)

        val medias: List<MediaMetadata> = mediaClient.getMediaMetadata(sortedBrands.mapNotNull { it.mediaId })

        return BrandAssembler.toBrands(sortedBrands, medias)
    }
}
