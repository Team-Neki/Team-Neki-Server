package com.yapp2app.map.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.map.domain.entity.PhotoBoothLocation
import com.yapp2app.map.infra.client.KakaoApiClient
import com.yapp2app.map.infra.persist.jpa.JpaBrandRepository
import com.yapp2app.map.infra.persist.jpa.JpaPhotoBoothLocationRepository
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
    private val kakaoApiClient: KakaoApiClient,
    private val brandRepository: JpaBrandRepository,
    private val photoBoothLocationRepository: JpaPhotoBoothLocationRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val geometryFactory = GeometryFactory(PrecisionModel(), 4326)

    data class CollectResult(val collectedCount: Int, val duplicatedCount: Int, val totalProcessed: Int)

    @Transactional
    fun execute(keyword: String, brandCode: String): CollectResult {
        log.info("Start collecting photo booth locations - keyword: {}, brandCode: {}", keyword, brandCode)

        // 브랜드 조회
        val brand = brandRepository.findByCode(brandCode)
            ?: throw IllegalArgumentException("Brand not found: $brandCode")

        // 기존 데이터 조회 및 Map으로 변환 (mapId를 키로)
        val existingLocations = photoBoothLocationRepository.findAllByBrandId(brand.id!!)
            .associateBy { it.mapId }
            .toMutableMap()

        log.info("Existing locations count: {}", existingLocations.size)

        val pageSize = 15

        // 1페이지 먼저 조회하여 pageableCount로 마지막 페이지 계산
        val firstPageResponse = kakaoApiClient.searchByKeyword(
            query = keyword,
            page = 1,
            size = pageSize,
        )

        val lastPage = firstPageResponse.meta.pageableCount

        log.info(
            "Total count: {}, Pageable count: {}, Last page: {}",
            firstPageResponse.meta.totalCount,
            pageSize,
            lastPage,
        )

        // 업데이트/삽입 대상 분류
        val toUpdate = mutableListOf<PhotoBoothLocation>()
        val toInsert = mutableListOf<PhotoBoothLocation>()
        val processedMapIds = mutableSetOf<String>()

        // 1페이지부터 마지막 페이지까지 순회
        for (page in 1..lastPage) {
            var retryCount = 0
            val maxRetries = 3
            var success = false

            while (!success && retryCount < maxRetries) {
                try {
                    // IP 차단 방지: 페이지 간 랜덤 딜레이 (500ms ~ 1000ms)
                    if (page > 1) {
                        val delayMillis = (500L..1000L).random()
                        Thread.sleep(delayMillis)
                        log.debug("Delayed {}ms before requesting page {}", delayMillis, page)
                    }

                    val response = kakaoApiClient.searchByKeyword(
                        query = keyword,
                        page = page,
                        size = pageSize,
                    )

                    log.info("Page {} - Documents: {}", page, response.documents.size)

                    response.documents.forEach { place ->
                        processedMapIds.add(place.id)

                        val longitude = place.longitude.toDouble()
                        val latitude = place.latitude.toDouble()
                        val point = geometryFactory.createPoint(Coordinate(longitude, latitude))

                        val existing = existingLocations[place.id]
                        if (existing != null) {
                            // 업데이트 대상
                            existing.brandId = brand.id
                            existing.name = place.placeName
                            existing.address = place.roadAddressName
                            existing.location = point
                            toUpdate.add(existing)
                        } else {
                            // 삽입 대상
                            val newLocation = PhotoBoothLocation(
                                mapId = place.id,
                                brandId = brand.id,
                                name = place.placeName,
                                address = place.roadAddressName,
                                location = point,
                            )
                            toInsert.add(newLocation)
                        }
                    }

                    success = true
                } catch (e: Exception) {
                    retryCount++
                    log.warn(
                        "Error occurred while collecting page {} (attempt {}/{}): {}",
                        page,
                        retryCount,
                        maxRetries,
                        e.message,
                    )

                    if (retryCount < maxRetries) {
                        // Exponential backoff: 2초, 4초, 8초...
                        val backoffMillis = (2000L * retryCount)
                        log.info("Retrying after {}ms...", backoffMillis)
                        Thread.sleep(backoffMillis)
                    } else {
                        log.error("Failed to collect page {} after {} attempts", page, maxRetries)
                        throw e
                    }
                }
            }
        }

        // 삭제 대상 (기존 데이터 중 API 결과에 없는 것들)
        val toDelete = existingLocations.values.filter { it.mapId !in processedMapIds }

        // 배치 쿼리 실행
        log.info("Batch processing - insert: {}, update: {}, delete: {}", toInsert.size, toUpdate.size, toDelete.size)

        if (toInsert.isNotEmpty()) {
            photoBoothLocationRepository.saveAll(toInsert)
            log.info("Inserted {} locations", toInsert.size)
        }

        if (toUpdate.isNotEmpty()) {
            photoBoothLocationRepository.saveAll(toUpdate)
            log.info("Updated {} locations", toUpdate.size)
        }

        if (toDelete.isNotEmpty()) {
            photoBoothLocationRepository.deleteAll(toDelete)
            log.info("Deleted {} locations", toDelete.size)
        }

        val totalProcessed = processedMapIds.size

        log.info(
            "Finish collecting - inserted: {}, updated: {}, deleted: {}, total: {}",
            toInsert.size,
            toUpdate.size,
            toDelete.size,
            totalProcessed,
        )

        return CollectResult(
            collectedCount = toInsert.size,
            duplicatedCount = toUpdate.size,
            totalProcessed = totalProcessed,
        )
    }
}
