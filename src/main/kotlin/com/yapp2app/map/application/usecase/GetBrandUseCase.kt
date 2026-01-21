package com.yapp2app.map.application.usecase

import com.yapp2app.auth.infra.security.properties.AppProperties
import com.yapp2app.common.annotation.UseCase
import com.yapp2app.map.application.port.BrandRepositoryPort
import com.yapp2app.map.application.result.GetBrandResult
import org.slf4j.LoggerFactory

/**
 * fileName       : GetBrandUseCase
 * author         : darren
 * date           : 2026. 1. 21. 14:41
 * description    : Brand 조회
 */
@UseCase
class GetBrandUseCase(private val brandRepository: BrandRepositoryPort, private val appProperties: AppProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(): List<GetBrandResult> {
        val brands = brandRepository.findAll()

        return brands.map {
            GetBrandResult(
                id = it.id,
                name = it.name,
                code = it.code,
                imageUrl = it.storageKey ?.let { "${appProperties.server.url}$IMAGE_URL_PATH$it" },
            )
        }
    }

    companion object {
        private const val IMAGE_URL_PATH = "/file/image/"
    }
}
