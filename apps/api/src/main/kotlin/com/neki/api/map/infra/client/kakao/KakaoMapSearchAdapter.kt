package com.neki.api.map.infra.client.kakao

import com.neki.config.kakao.KakaoApiRateLimitProperties
import com.neki.domain.map.external.MapApiClient
import com.neki.domain.map.external.MapSearch
import com.neki.domain.map.models.GeographicKoreaBounds
import com.neki.domain.map.models.SearchedPlace
import com.neki.domain.map.models.SearchedPlaces
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * fileName       : KakaoMapSearchAdapter
 * author         : darren
 * date           : 2026. 01. 22.
 * description    : 카카오 맵 검색 서비스 (Rate limiting, Retry 로직 포함)
 */
@Service
class KakaoMapSearchAdapter(private val mapApiClient: MapApiClient) : MapSearch {
    private val log: Logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val PAGE_SIZE = 15
        private const val MAX_RETRIES = 5
    }

    /**
     * 한국 전역을 그리드로 나누어 검색
     */
    override fun searchAllKorea(keyword: String): List<SearchedPlace> {
        val grids: List<GeographicKoreaBounds> = GeographicKoreaBounds.divideIntoGrids()
        val allPlaces = mutableMapOf<String, SearchedPlace>()

        grids.forEachIndexed { index, grid ->
            if (index > 0) {
                val delayMin = KakaoApiRateLimitProperties.DEFAULT.gridDelayMin
                val delayMax = KakaoApiRateLimitProperties.DEFAULT.gridDelayMax
                val gridDelay: Long = (delayMin..delayMax).random()
                log.debug("Waiting {}ms before next grid...", gridDelay)
                Thread.sleep(gridDelay)
            }

            log.info("Processing grid {}/{} - rect: {}", index + 1, grids.size, grid.toRectString())

            val gridPlaces: List<SearchedPlace> = searchInGrid(keyword, grid.toRectString())
            gridPlaces.forEach { place ->
                allPlaces[place.id] = place
            }

            log.info(
                "Grid {}/{} completed - collected: {}, total so far: {}",
                index + 1,
                grids.size,
                gridPlaces.size,
                allPlaces.size,
            )
        }

        return allPlaces.values.toList()
    }

    /**
     * 특정 그리드 내에서 검색 (페이징 처리 포함)
     */
    private fun searchInGrid(keyword: String, rect: String): List<SearchedPlace> {
        // 첫 API 호출 전 딜레이
        Thread.sleep(KakaoApiRateLimitProperties.DEFAULT.initialDelay)
        log.debug("Initial delay {}ms before first API call", KakaoApiRateLimitProperties.DEFAULT.initialDelay)

        // 첫 페이지 조회
        val firstPageResponse: SearchedPlaces = fetchPageWithRetry(keyword, 1, rect, skipDelay = true)
        val lastPage = firstPageResponse.pagination.pageableCount

        log.debug(
            "Total count: {}, Pageable count: {}, Last page: {}",
            firstPageResponse.pagination.totalCount,
            PAGE_SIZE,
            lastPage,
        )

        val allPlaces = mutableListOf<SearchedPlace>()
        var previousPageIds: Set<String>? = null

        // 첫 페이지 처리
        val firstPageIds: Set<String> = firstPageResponse.places.map { it.id }.toSet()
        allPlaces.addAll(firstPageResponse.places)
        previousPageIds = firstPageIds

        // 2페이지부터 조회
        for (page in 2..lastPage) {
            val response: SearchedPlaces = fetchPageWithRetry(keyword, page, rect)

            log.debug("Page {} - Documents: {}", page, response.places.size)

            val currentPageIds: Set<String> = response.places.map { it.id }.toSet()
            if (currentPageIds == previousPageIds) {
                log.debug("Page {} has same results as previous page. Stopping pagination.", page)
                break
            }
            previousPageIds = currentPageIds

            allPlaces.addAll(response.places)
        }

        return allPlaces
    }

    /**
     * Retry 로직이 포함된 API 호출
     */
    private fun fetchPageWithRetry(
        keyword: String,
        page: Int,
        rect: String,
        skipDelay: Boolean = false,
    ): SearchedPlaces {
        repeat(MAX_RETRIES) { attempt ->
            try {
                // 페이지 간 딜레이
                if (!skipDelay && page > 1) {
                    val pageDelayMin = KakaoApiRateLimitProperties.DEFAULT.pageDelayMin
                    val pageDelayMax = KakaoApiRateLimitProperties.DEFAULT.pageDelayMax
                    val delayMillis: Long = (pageDelayMin..pageDelayMax).random()
                    Thread.sleep(delayMillis)
                    log.debug("Delayed {}ms before requesting page {}", delayMillis, page)
                }

                return mapApiClient.searchByKeyword(
                    query = keyword,
                    page = page,
                    size = PAGE_SIZE,
                    rect = rect,
                )
            } catch (e: Exception) {
                val attemptNumber = attempt + 1
                log.warn(
                    "Error occurred while collecting page {} (attempt {}/{}): {}",
                    page,
                    attemptNumber,
                    MAX_RETRIES,
                    e.message,
                )

                // 마지막 시도였다면 예외를 던짐
                if (attemptNumber >= MAX_RETRIES) {
                    log.error("Failed to collect page {} after {} attempts", page, MAX_RETRIES)
                    throw e
                }

                // Exponential backoff
                val backoffMillis = KakaoApiRateLimitProperties.DEFAULT.retryBackoffBase * attemptNumber
                log.info("Rate limited or error occurred. Retrying after {}ms...", backoffMillis)
                Thread.sleep(backoffMillis)
            }
        }

        throw IllegalStateException("Unexpected state in fetchPageWithRetry")
    }
}
