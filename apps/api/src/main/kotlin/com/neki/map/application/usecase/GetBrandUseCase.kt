package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.map.BrandOrderPolicy
import com.neki.map.application.dto.MapResult
import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.application.port.MediaClientPort
import com.neki.map.application.port.UserBrandOrderRepositoryPort
import com.neki.map.entity.Brand
import com.neki.photo.application.port.dto.MediaContract
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : GetBrandUseCase
 * author         : darren
 * date           : 2026. 1. 21. 14:41
 * description    : Brand 조회
 */
@UseCase
class GetBrandUseCase(
    private val brandRepository: BrandRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val userBrandOrderRepository: UserBrandOrderRepositoryPort,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(userId: Long): List<MapResult.GetBrand> {
        val brands: List<Brand> = brandRepository.findAll()

        val sortOrderMap: Map<Long, Int> = userBrandOrderRepository.findSortOrderMapByUserId(userId)
        val sortedBrands: List<Brand> = BrandOrderPolicy.sort(brands, sortOrderMap)

        val mediaStorageInfos: List<MediaContract.StorageInfo> = mediaClient.getMediaStorageInfos(
            sortedBrands.mapNotNull {
                it.mediaId
            },
        )

        val mediaByMediaId = mediaStorageInfos.associateBy { it.mediaId }

        return sortedBrands.map { brand ->
            MapResult.GetBrand(
                id = brand.id!!,
                name = brand.name,
                code = brand.code,
                storageKey = brand.mediaId?.let { mediaByMediaId[it]?.storageKey },
            )
        }
    }
}
