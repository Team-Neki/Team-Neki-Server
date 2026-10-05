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

/**
 * fileName       : GetSearchFilterUseCase
 * author         : koo
 * date           : 2026. 9. 15. 오후 5:29
 * description    : 부스 목록에서 쓸 수 있는 브랜드 필터 조회. 흐름은 부스 목록과 같고 마지막에 브랜드별로 센다.
 */
@UseCase
class GetSearchFilterUseCase(
    private val queryUnderstandingService: QueryUnderstandingService,
    private val photoBoothSearchService: PhotoBoothSearchService,
    private val mapClient: MapClient,
) {

    fun execute(query: SearchQuery.GetFilter): SearchResult.GetFilter {
        // 1. Query understanding : 검색어 -> QueryIntent (지역·역·브랜드)
        val intent: QueryIntent = queryUnderstandingService.understand(query.keyword)

        // 2. Retrieval : 인식한 지역·역의 색인 행. 지역·역을 찾지 못하면 빈 목록
        val indexedBooths: List<PhotoBoothSearch> = photoBoothSearchService.findIndexedBooths(query, intent)

        // 3. Map booth : 목록과 같은 부스만 세야 칩의 개수와 목록 건수가 맞는다
        val mapBooths: List<MapBooth> = mapClient.findMapBooths(query.userId, indexedBooths)
        val booths: SearchedBooths = SearchedBooths.of(indexedBooths, mapBooths)

        // 4. Brand order : 사용자별 브랜드 순서 (map)
        val orderedBrandIds: List<Long> = mapClient.findOrderedBrandIds(query.userId)

        // 5. Counting, Assembly : 브랜드별 개수를 사용자 순서로
        return SearchAssembler.toBrandFilter(booths.countByBrand(orderedBrandIds))
    }
}
