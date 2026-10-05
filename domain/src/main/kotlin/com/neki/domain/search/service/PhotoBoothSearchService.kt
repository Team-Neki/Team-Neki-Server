package com.neki.domain.search.service

import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.CompletionKeyword
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SearchCondition
import com.neki.domain.search.models.qu.QueryIntent
import com.neki.domain.search.repository.PhotoBoothSearchRepository
import com.neki.domain.search.service.qu.SearchNormalizer
import org.springframework.stereotype.Component

/**
 * fileName       : PhotoBoothSearchService
 * author         : koo
 * date           : 2026. 9. 30.
 * description    : 검색 색인 조회. 부스 자동완성과, QU 가 이해한 지역·역·지점·브랜드로 찾는 부스 목록(검색 정책 7장의 지도검색 범위).
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

    /**
     * 부스 자동완성. 정규화하면 1자 이하인 검색어(`강`, `강_`)는 걸리는 것이 너무 많아 조회하지 않고 빈 결과를 준다.
     * 낱말 규칙은 지역·역 탭과 같은 검색어를 받기 위한 것이다 (CompletionKeyword.boothTerms).
     */
    fun searchByKeyword(query: SearchQuery.SearchPhotoBoothsByKeyword): PageWithTotalCount<PhotoBoothSearch> {
        if (SearchNormalizer.normalize(query.keyword).length < SearchQuery.MIN_COMPLETION_KEYWORD_LENGTH) {
            return query.pagination.slice(emptyList(), 0L)
        }

        val terms: List<List<String>> = CompletionKeyword.boothTerms(query.keyword)
        val fetched: List<PhotoBoothSearch> =
            photoBoothSearchRepository.findByKeyword(query.keyword, terms, query.userLocation, query.pagination)
        val totalCount: Long = photoBoothSearchRepository.countByKeyword(query.keyword, terms)

        return query.pagination.slice(fetched, totalCount)
    }
}
