package com.neki.map.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.map.application.dto.MapCommand
import com.neki.map.application.dto.MapResult
import com.neki.map.application.port.BrandRepositoryPort
import com.neki.map.application.port.MapSearchPort
import com.neki.map.application.port.PhotoBoothLocationRepositoryPort
import com.neki.map.application.port.dto.MapContract
import com.neki.map.domain.entity.PhotoBoothLocation
import com.neki.map.domain.vo.GeoPoint
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : CollectPhotoBoothLocationUseCase
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 위치 정보 수집 UseCase
 */
@UseCase
class CollectPhotoBoothLocationUseCase(
    private val brandRepository: BrandRepositoryPort,
    private val photoBoothLocationRepository: PhotoBoothLocationRepositoryPort,
    private val transactionRunner: TransactionRunner,
    private val mapSearch: MapSearchPort,
) {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(command: MapCommand.CollectPhotoBooth): MapResult.CollectPhotoBooth {
        log.info(
            "Start collecting photo booth locations - keyword: {}, brandCode: {}",
            command.keyword,
            command.brandCode,
        )

        // 1. 브랜드 및 기존 위치 조회
        val (brand, existingLocations) = transactionRunner.readOnly {
            val brand = brandRepository.getBrand(command.brandCode)
                ?: throw BusinessException(ResultCode.NOT_FOUND)
            val locations = photoBoothLocationRepository.getPhotoBoothLocations(brand.id!!)
                .associateBy { it.mapId }
            brand to locations
        }

        log.info("Existing locations count: {}", existingLocations.size)

        // 2. 카카오 API에서 검색 (인프라 계층에서 rate limiting, retry 처리)
        val places: List<MapContract.LocalSearchResult.Place> = mapSearch.searchAllKorea(command.keyword)

        // 3. 도메인 엔티티로 변환
        val processed = places.associateBy({ it.id }) { place ->
            mapToPhotoBoothLocation(place, brand.id!!, existingLocations[place.id])
        }

        // 4. 변경 사항 저장
        return persistChanges(processed, existingLocations)
    }

    private fun mapToPhotoBoothLocation(
        place: MapContract.LocalSearchResult.Place,
        brandId: Long,
        existing: PhotoBoothLocation?,
    ): PhotoBoothLocation {
        val point = GeoPoint.of(place.latitude.toDouble(), place.longitude.toDouble()).point

        return existing?.apply {
            this.brandId = brandId
            this.branchName = place.placeName
            this.address = place.roadAddressName
            this.location = point
        } ?: PhotoBoothLocation(
            mapId = place.id,
            brandId = brandId,
            branchName = place.placeName,
            address = place.roadAddressName,
            location = point,
        )
    }

    private fun persistChanges(
        processed: Map<String, PhotoBoothLocation>,
        existingLocations: Map<String, PhotoBoothLocation>,
    ): MapResult.CollectPhotoBooth {
        val insertCount: Int = processed.keys.count { it !in existingLocations }
        val updateCount: Int = processed.keys.count { it in existingLocations }
        val toDelete: List<PhotoBoothLocation> = existingLocations.values.filter { it.mapId !in processed }

        log.info("Batch processing - insert: {}, update: {}, delete: {}", insertCount, updateCount, toDelete.size)

        transactionRunner.run {
            if (processed.isNotEmpty()) {
                photoBoothLocationRepository.saveAll(processed.values)
                log.info("Saved {} locations (insert: {}, update: {})", processed.size, insertCount, updateCount)
            }

            if (toDelete.isNotEmpty()) {
                photoBoothLocationRepository.deleteAll(toDelete)
                log.info("Deleted {} locations", toDelete.size)
            }
        }

        log.info(
            "Finish collecting - inserted: {}, updated: {}, deleted: {}, total: {}",
            insertCount,
            updateCount,
            toDelete.size,
            processed.size,
        )

        return MapResult.CollectPhotoBooth(
            collectedCount = insertCount,
            duplicatedCount = updateCount,
            totalProcessed = processed.size,
        )
    }
}
