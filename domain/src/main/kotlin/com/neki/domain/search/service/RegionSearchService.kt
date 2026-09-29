package com.neki.domain.search.service

import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.LegalDong
import com.neki.domain.search.models.SidoAlias
import com.neki.domain.search.repository.LegalDongRepository
import org.springframework.stereotype.Component

/**
 * fileName       : RegionSearchService
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 법정동 이름·전체 경로 접두 검색
 */
@Component
class RegionSearchService(private val legalDongRepository: LegalDongRepository) {

    fun search(query: SearchQuery.SearchRegions): PageWithTotalCount<LegalDong> {
        val name: String = query.keyword
        if (name.length < MIN_KEYWORD_LENGTH) return query.pagination.slice(emptyList(), 0L)

        // 전체 경로는 시도 이름으로 시작하고 공백 뒤에 다음 계층이 온다. 공백이 없으면 아직 시도 이름을
        // 치는 중이라 경로로 찾지 않는다. `서울`, `충청북도` 만으로 시도 아래 구역이 다 쏟아지지 않게 한다.
        // 이름 검색은 원래 검색어로 한다. 줄임말을 바꾸면 `광주` 로 경기도 광주시를 못 찾는다
        val path: String? = if (name.contains(' ')) SidoAlias.expand(name) else null

        val fetched: List<LegalDong> = legalDongRepository.findByNameOrPathPrefix(name, path, query.pagination)
        val totalCount: Long = legalDongRepository.countByNameOrPathPrefix(name, path)

        return query.pagination.slice(fetched, totalCount)
    }

    companion object {
        /** 1자는 걸리는 구역이 너무 많아 조회하지 않고 빈 결과를 준다 */
        private const val MIN_KEYWORD_LENGTH = 2
    }
}
