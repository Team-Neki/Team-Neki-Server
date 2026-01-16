package com.yapp2app.map.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.map.application.contract.KakaoLocalSearchResponse
import com.yapp2app.map.application.contract.KakaoPlace
import com.yapp2app.map.application.port.BrandRepositoryPort
import com.yapp2app.map.application.port.MapApiClientPort
import com.yapp2app.map.application.port.PhotoBoothLocationRepositoryPort
import com.yapp2app.map.application.result.Rectangle
import com.yapp2app.map.domain.entity.PhotoBoothLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : CollectPhotoBoothUseCase
 * author         : darren
 * date           : 2026. 01. 13.
 * description    : 포토부스 위치 정보 수집 UseCase
 */
@UseCase
class CollectPhotoBoothUseCase(
    private val mapApiClient: MapApiClientPort,
    private val brandRepository: BrandRepositoryPort,
    private val photoBoothLocationRepository: PhotoBoothLocationRepositoryPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    companion object {
        private const val PAGE_SIZE = 15
        private const val MAX_RETRIES = 5
        private const val GRID_SIZE = 0.25

        // Rate limiting 설정
        private const val PAGE_DELAY_MIN = 1000L // 페이지 간 최소 딜레이 (ms)
        private const val PAGE_DELAY_MAX = 2000L // 페이지 간 최대 딜레이 (ms)
        private const val GRID_DELAY_MIN = 2000L // 그리드 간 최소 딜레이 (ms)
        private const val GRID_DELAY_MAX = 4000L // 그리드 간 최대 딜레이 (ms)

        // 대한민국 좌표 범위
        private const val KOREA_MIN_LAT = 33.0 // 최남단 (제주 마라도 근처)
        private const val KOREA_MAX_LAT = 38.6 // 최북단 (DMZ 근처)
        private const val KOREA_MIN_LNG = 124.5 // 최서단 (백령도 근처)
        private const val KOREA_MAX_LNG = 132.0 // 최동단 (독도 근처)
    }

    data class CollectResult(val collectedCount: Int, val duplicatedCount: Int, val totalProcessed: Int)

    @Transactional
    fun execute(keyword: String, brandCode: String): CollectResult {
        log.info("Start collecting photo booth locations - keyword: {}, brandCode: {}", keyword, brandCode)

        val brand = brandRepository.getBrand(brandCode)
            ?: throw IllegalArgumentException("Brand not found: $brandCode")

        val existingLocations = photoBoothLocationRepository.getPhotoBoothLocations(brand.id!!)
            .associateBy { it.mapId }
        log.info("Existing locations count: {}", existingLocations.size)

        val processed = collectFromAllGrids(keyword, brand.id, existingLocations)

        return persistChanges(processed, existingLocations)
    }

    private fun collectFromAllGrids(
        keyword: String,
        brandId: Long,
        existingLocations: Map<String, PhotoBoothLocation>,
    ): Map<String, PhotoBoothLocation> {
        val grids = divideKoreaIntoGrids(GRID_SIZE)
        val processed = mutableMapOf<String, PhotoBoothLocation>()

        grids.forEachIndexed { index, grid ->
            // 그리드 간 딜레이 (첫 번째 그리드 제외)
            if (index > 0) {
                val gridDelay = (GRID_DELAY_MIN..GRID_DELAY_MAX).random()
                log.debug("Waiting {}ms before next grid...", gridDelay)
                Thread.sleep(gridDelay)
            }

            log.info("Processing grid {}/{} - rect: {}", index + 1, grids.size, grid.toRect())

            val gridResult = collectFromApi(keyword, brandId, existingLocations, grid)
            processed.putAll(gridResult)

            log.info(
                "Grid {}/{} completed - collected: {}, total so far: {}",
                index + 1,
                grids.size,
                gridResult.size,
                processed.size,
            )
        }

        return processed
    }

    private fun collectFromApi(
        keyword: String,
        brandId: Long,
        existingLocations: Map<String, PhotoBoothLocation>,
        grid: Rectangle,
    ): Map<String, PhotoBoothLocation> {
        val rect = grid.toRect()
        val lastPage = fetchLastPage(keyword, rect)
        val processed = mutableMapOf<String, PhotoBoothLocation>()
        var previousPageIds: Set<String>? = null

        pageLoop@ for (page in 1..lastPage) {
            val response = fetchPageWithRetry(keyword, page, rect)

            log.debug("Page {} - Documents: {}", page, response.documents.size)

            val currentPageIds = response.documents.map { it.id }.toSet()
            if (currentPageIds == previousPageIds) {
                log.debug("Page {} has same results as previous page. Stopping pagination.", page)
                break@pageLoop
            }
            previousPageIds = currentPageIds

            response.documents.forEach { place ->
                processed[place.id] = mapToPhotoBoothLocation(place, brandId, existingLocations[place.id])
            }
        }

        return processed
    }

    private fun fetchLastPage(keyword: String, rect: String): Int {
        val firstPageResponse = mapApiClient.kakaoSearchByKeyword(
            query = keyword,
            page = 1,
            size = PAGE_SIZE,
            rect = rect,
        )

        log.debug(
            "Total count: {}, Pageable count: {}, Last page: {}",
            firstPageResponse.meta.totalCount,
            PAGE_SIZE,
            firstPageResponse.meta.pageableCount,
        )

        return firstPageResponse.meta.pageableCount
    }

    private fun fetchPageWithRetry(keyword: String, page: Int, rect: String): KakaoLocalSearchResponse {
        var retryCount = 0

        while (retryCount < MAX_RETRIES) {
            try {
                // 페이지 간 딜레이
                if (page > 1) {
                    val delayMillis = (PAGE_DELAY_MIN..PAGE_DELAY_MAX).random()
                    Thread.sleep(delayMillis)
                    log.debug("Delayed {}ms before requesting page {}", delayMillis, page)
                }

                return mapApiClient.kakaoSearchByKeyword(
                    query = keyword,
                    page = page,
                    size = PAGE_SIZE,
                    rect = rect,
                )
            } catch (e: Exception) {
                retryCount++
                log.warn(
                    "Error occurred while collecting page {} (attempt {}/{}): {}",
                    page,
                    retryCount,
                    MAX_RETRIES,
                    e.message,
                )

                if (retryCount < MAX_RETRIES) {
                    // Exponential backoff: 5초, 10초, 15초, 20초...
                    val backoffMillis = 5000L * retryCount
                    log.info("Rate limited. Retrying after {}ms...", backoffMillis)
                    Thread.sleep(backoffMillis)
                } else {
                    log.error("Failed to collect page {} after {} attempts", page, MAX_RETRIES)
                    throw e
                }
            }
        }

        throw IllegalStateException("Unexpected state in fetchPageWithRetry")
    }

    private fun Rectangle.toRect(): String = "$x1,$y1,$x2,$y2"

    private fun mapToPhotoBoothLocation(
        place: KakaoPlace,
        brandId: Long,
        existing: PhotoBoothLocation?,
    ): PhotoBoothLocation {
        val point = geometryFactory.createPoint(
            Coordinate(place.longitude.toDouble(), place.latitude.toDouble()),
        )

        return existing?.apply {
            this.brandId = brandId
            this.name = place.placeName
            this.address = place.roadAddressName
            this.location = point
        } ?: PhotoBoothLocation(
            mapId = place.id,
            brandId = brandId,
            name = place.placeName,
            address = place.roadAddressName,
            location = point,
        )
    }

    private fun persistChanges(
        processed: Map<String, PhotoBoothLocation>,
        existingLocations: Map<String, PhotoBoothLocation>,
    ): CollectResult {
        val insertCount = processed.keys.count { it !in existingLocations }
        val updateCount = processed.keys.count { it in existingLocations }
        val toDelete = existingLocations.values.filter { it.mapId !in processed }

        log.info("Batch processing - insert: {}, update: {}, delete: {}", insertCount, updateCount, toDelete.size)

        if (processed.isNotEmpty()) {
            photoBoothLocationRepository.saveAll(processed.values)
            log.info("Saved {} locations (insert: {}, update: {})", processed.size, insertCount, updateCount)
        }

        if (toDelete.isNotEmpty()) {
            photoBoothLocationRepository.deleteAll(toDelete)
            log.info("Deleted {} locations", toDelete.size)
        }

        log.info(
            "Finish collecting - inserted: {}, updated: {}, deleted: {}, total: {}",
            insertCount,
            updateCount,
            toDelete.size,
            processed.size,
        )

        return CollectResult(
            collectedCount = insertCount,
            duplicatedCount = updateCount,
            totalProcessed = processed.size,
        )
    }

    /**
     * 대한민국을 그리드로 분할
     * @param gridSize 그리드 크기 (도 단위, 예: 0.1 = 약 10km)
     * @return 그리드 사각형 리스트
     */
    fun divideKoreaIntoGrids(gridSize: Double): MutableList<Rectangle> {
        val grids: MutableList<Rectangle> = ArrayList<Rectangle>()

        var lat = KOREA_MIN_LAT
        while (lat < KOREA_MAX_LAT) {
            var lng = KOREA_MIN_LNG
            while (lng < KOREA_MAX_LNG) {
                val grid = Rectangle(x1 = lng, y1 = lat, x2 = lng + gridSize, y2 = lat + gridSize)

                grids.add(grid)
                lng += gridSize
            }
            lat += gridSize
        }

        log.info("Created {} grids with size {}", grids.size, gridSize)
        return grids
    }
}
