package com.neki.api.search.application

import com.neki.api.search.application.dto.SearchAssembler
import com.neki.api.search.application.dto.SearchResult
import com.neki.core.annotation.UseCase
import com.neki.domain.search.client.MapClient
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.MapBooth
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchedBooths
import com.neki.domain.search.models.qu.QueryIntent
import com.neki.domain.search.service.PhotoBoothSearchService
import com.neki.domain.search.service.qu.QueryUnderstandingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * fileName       : SearchPhotoBoothsUseCase
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:28
 * description    : 고른 지역·역의 부스 목록 조회.
 *   흐름은 이해(QU) -> 색인 조회 -> 지도 부스(map) -> 조립이며, 이해의 하위 단계(정규화, NER, Intent)는 QU 가 갖는다.
 */
@UseCase
class SearchPhotoBoothsUseCase(
    private val queryUnderstandingService: QueryUnderstandingService,
    private val photoBoothSearchService: PhotoBoothSearchService,
    private val mapClient: MapClient,
) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    fun execute(query: SearchQuery.GetPhotoBooths): SearchResult.GetPhotoBooths {
        // 1. Query understanding : 검색어 -> QueryIntent (지역·역·브랜드)
        val intent: QueryIntent = queryUnderstandingService.understand(query.keyword)

        // 2. Retrieval : 인식한 지역·역의 색인 행. 지역·역을 찾지 못하면 빈 목록
        val indexedBooths: List<PhotoBoothSearch> = photoBoothSearchService.findIndexedBooths(query, intent)

        // 3. Map booth : 지도 부스 id 와 즐겨찾기는 map 도메인이 정본이다. 지도에 없거나 숨긴 지점은 빠진다
        val mapBooths: List<MapBooth> = mapClient.findMapBooths(query.userId, indexedBooths)
        val booths: SearchedBooths = SearchedBooths.of(indexedBooths, mapBooths)

        log.info(
            "[SEARCH] api=photo-booths keyword=\"{}\" entities={} remainingTerms={} indexed={} results={}",
            query.keyword,
            intent.entities.map { "${it.type}:${it.keyword}" },
            intent.remainingTerms,
            indexedBooths.size,
            booths.size,
        )

        // 4. Ordering, Assembly : 검색 정책 12장 순서
        return SearchAssembler.toPhotoBooths(booths.ordered(query.userLocation), query.userLocation)
    }
}
