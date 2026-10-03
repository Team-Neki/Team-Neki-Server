package com.neki.domain.search.client

import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.PhotoBoothSummary

/**
 * fileName       : PhotoBoothClient
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 포토부스를 가진 map 도메인 호출을 위한 인터페이스
 */
interface PhotoBoothClient {

    /**
     * 검색어 전체가 이름 앞부분이거나, 낱말마다 브랜드명·지점명·주소 중 하나에 들어 있는 포토부스 한 페이지와 전체 건수.
     * 이름 앞부분으로 걸린 것이 먼저 온다.
     */
    fun searchByKeyword(query: SearchQuery.SearchPhotoBoothsByKeyword): PageWithTotalCount<PhotoBoothSummary>

    /** 삭제되지 않은 브랜드 이름. 지역·역 검색어에서 브랜드 낱말을 빼는 데 쓴다 */
    fun findBrandNames(): List<String>
}
