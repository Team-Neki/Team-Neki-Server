package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.application.port.MediaClientPort
import com.neki.map.application.port.UserBrandOrderRepositoryPort
import com.neki.map.application.result.GetBrandResult
import com.neki.map.domain.entity.Brand
import com.neki.photo.application.contract.MediaStorageInfo
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

    fun execute(userId: Long): List<GetBrandResult> {
        val brands: List<Brand> = brandRepository.findAll()

        // 사용자가 정렬을 커스텀한 브랜드는 sortOrder 순으로, 그 외(저장 이후 추가된 브랜드 등)는 뒤쪽에 id 순으로 정렬한다.
        // 저장된 순서가 없으면 모두 동일하게 취급되어 brandRepository.findAll()의 기본 정렬(id 오름차순)을 따른다.
        val sortOrderMap: Map<Long, Int> = userBrandOrderRepository.findSortOrderMapByUserId(userId)
        val sortedBrands: List<Brand> = brands.sortedWith(
            compareBy({ sortOrderMap[it.id] ?: Int.MAX_VALUE }, { it.id }),
        )

        val mediaStorageInfos: List<MediaStorageInfo> = mediaClient.getMediaStorageInfos(
            sortedBrands.mapNotNull {
                it.mediaId
            },
        )

        val mediaByMediaId = mediaStorageInfos.associateBy { it.mediaId }

        return sortedBrands.map { brand ->
            GetBrandResult(
                id = brand.id!!,
                name = brand.name,
                code = brand.code,
                storageKey = brand.mediaId?.let { mediaByMediaId[it]?.storageKey },
            )
        }
    }
}
