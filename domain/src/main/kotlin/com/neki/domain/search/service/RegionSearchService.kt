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
        // 이름 검색은 원래 검색어로 한다. `광주` 를 바꿔 버리면 경기도 광주시가 이름으로 걸리지 않는다
        val name: String = query.keyword
        val path: String = SidoAlias.expand(query.keyword)

        val fetched: List<LegalDong> = legalDongRepository.findByNameOrPathPrefix(name, path, query.pagination)
        val totalCount: Long = legalDongRepository.countByNameOrPathPrefix(name, path)

        return query.pagination.slice(fetched, totalCount)
    }
}
