package com.neki.domain.search.service

import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchCondition
import com.neki.domain.search.models.qu.QueryIntent
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import org.springframework.stereotype.Component

/**
 * fileName       : PhotoBoothSearchService
 * author         : koo
 * date           : 2026. 9. 30.
 * description    : QU 가 이해한 지역·역·지점·브랜드로 검색 색인을 조회한다. 검색 정책 7장의 지도검색 범위를 따른다.
 */
@Component
class PhotoBoothSearchService(private val photoBoothSearchRepository: PhotoBoothSearchRepository) {

    /**
     * intent 는 유스케이스가 QU 로 먼저 만든다. 범위(지역·역·지점)를 찾지 못했거나 딸린 행이 없으면 빈 목록.
     * 지원하지 않는 검색어와 결과 없음은 클라이언트에서 같은 결과 없음 화면이다 (검색 정책 20장).
     */
    fun findIndexedBooths(scope: SearchQuery.PhotoBoothScope, intent: QueryIntent): List<PhotoBoothSearch> {
        val condition: SearchCondition = SearchCondition.of(intent, scope.brandIds) ?: return emptyList()
        if (condition.matchesNothing) return emptyList()

        return condition.scopes
            .flatMap { photoBoothSearchRepository.findByScope(it, condition.brandIds) }
            .distinctBy { it.id }
    }
}
