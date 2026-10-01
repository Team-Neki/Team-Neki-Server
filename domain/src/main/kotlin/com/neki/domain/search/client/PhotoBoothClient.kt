package com.neki.domain.search.client

import com.neki.core.domain.vo.PageWithTotalCount
import com.neki.domain.search.dto.SearchQuery
import com.neki.domain.search.models.PhotoBoothName

/**
 * fileName       : PhotoBoothClient
 * author         : darren
 * date           : 2026. 10. 1.
 * description    : 포토부스를 가진 map 도메인 호출을 위한 인터페이스
 */
interface PhotoBoothClient {

    /** 지점명, 브랜드명, `브랜드명 지점명` 중 하나가 keyword 로 시작하는 포토부스 한 페이지와 전체 건수 */
    fun searchByName(query: SearchQuery.SearchPhotoBoothsByKeyword): PageWithTotalCount<PhotoBoothName>
}
