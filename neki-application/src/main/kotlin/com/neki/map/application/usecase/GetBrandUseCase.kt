package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.application.port.MediaClientPort
import com.neki.map.application.result.GetBrandResult
import com.neki.map.entity.Brand
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
class GetBrandUseCase(private val brandRepository: BrandRepositoryPort, private val mediaClient: MediaClientPort) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(): List<GetBrandResult> {
        val brands: List<Brand> = brandRepository.findAll()

        val mediaStorageInfos: List<MediaStorageInfo> = mediaClient.getMediaStorageInfos(
            brands.mapNotNull {
                it.mediaId
            },
        )

        val mediaByMediaId = mediaStorageInfos.associateBy { it.mediaId }

        return brands.map { brand ->
            GetBrandResult(
                id = brand.id!!,
                name = brand.name,
                code = brand.code,
                storageKey = brand.mediaId?.let { mediaByMediaId[it]?.storageKey },
            )
        }
    }
}
