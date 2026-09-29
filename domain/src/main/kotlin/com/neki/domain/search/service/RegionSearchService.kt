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
        val keyword: String = SidoAlias.expand(query.keyword)

        val fetched: List<LegalDong> = legalDongRepository.findByNamePrefix(keyword, query.pagination)
        val totalCount: Long = legalDongRepository.countByNamePrefix(keyword)

        return query.pagination.slice(fetched, totalCount)
    }
}
