package com.neki.api.map.application

import com.neki.api.map.application.dto.MapResult
import com.neki.core.annotation.UseCase
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.map.dto.MapCommand
import com.neki.domain.map.external.MapSearch
import com.neki.domain.map.models.Brand
import com.neki.domain.map.models.GeoPoint
import com.neki.domain.map.models.PhotoBoothLocation
import com.neki.domain.map.models.SearchedPlace
import com.neki.domain.map.service.BrandService
import com.neki.domain.map.service.MapService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : CollectPhotoBoothLocationUseCase
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 위치 정보 수집 UseCase
 */
@Deprecated("Prefect 배치로 이관 예정. 이관 완료 후 POST /api/photo-booths/collect 와 함께 제거한다")
@UseCase
class CollectPhotoBoothLocationUseCase(
    private val brandService: BrandService,
    private val mapService: MapService,
    private val transactionRunner: TransactionRunner,
    private val mapSearch: MapSearch,
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
            val brand: Brand = brandService.getBrand(command)
            val locations: Map<String, PhotoBoothLocation> = mapService.getLocationsByBrandId(brand.id!!)
                .associateBy { it.mapId }
            brand to locations
        }

        log.info("Existing locations count: {}", existingLocations.size)

        // 2. 카카오 API에서 검색 (인프라 계층에서 rate limiting, retry 처리)
        val places: List<SearchedPlace> = mapSearch.searchAllKorea(command.keyword)

        // 3. 도메인 엔티티로 변환
        val processed = places.associateBy({ it.id }) { place ->
            mapToPhotoBoothLocation(place, brand.id!!, existingLocations[place.id])
        }

        // 4. 변경 사항 저장
        return persistChanges(processed, existingLocations)
    }

    private fun mapToPhotoBoothLocation(
        place: SearchedPlace,
        brandId: Long,
        existing: PhotoBoothLocation?,
    ): PhotoBoothLocation {
        val point = GeoPoint.of(place.latitude.toDouble(), place.longitude.toDouble()).point

        return existing
            ?.apply { refreshPlace(brandId, place.placeName, place.roadAddressName, point) }
            ?: PhotoBoothLocation(
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
                mapService.saveLocations(processed.values)
                log.info("Saved {} locations (insert: {}, update: {})", processed.size, insertCount, updateCount)
            }

            if (toDelete.isNotEmpty()) {
                mapService.deleteLocations(toDelete)
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
