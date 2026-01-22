package com.yapp2app.map.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.properties.AppProperties
import com.yapp2app.map.application.port.BrandRepositoryPort
import com.yapp2app.map.application.result.GetBrandResult
import com.yapp2app.media.api.controller.FileController.Companion.IMAGE_URL_PATH
import com.yapp2app.photo.application.port.MediaClientPort
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
    private val appProperties: AppProperties,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(): List<GetBrandResult> {
        val brands = brandRepository.findAll()

        val mediaStorageInfos = mediaClient.getMediaStorageInfos(brands.mapNotNull { it.mediaId })

        val mediaByMediaId = mediaStorageInfos.associateBy { it.mediaId }

        return brands.map { brand ->
            val storageKey = brand.mediaId?.let { mediaByMediaId[it]?.storageKey }
            GetBrandResult(
                id = brand.id!!,
                name = brand.name,
                code = brand.code,
                imageUrl = storageKey?.let { "${appProperties.server.url}$IMAGE_URL_PATH$it" },
            )
        }
    }
}
