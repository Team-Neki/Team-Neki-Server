package com.neki.domain.search.repository

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.LegalDong

/**
 * fileName       : LegalDongRepository
 * author         : darren
 * date           : 2026. 9. 25.
 * description    : 법정동 조회
 */
interface LegalDongRepository {

    /**
     * 이름이 namePrefix 로 시작하거나 전체 경로가 pathPrefix 로 시작하는 시군구 이하 법정동.
     * pathPrefix 가 null 이면 이름으로만 찾는다. 전체 경로로 걸리면 그 아래 구역도 함께 나온다. 계층이 위인 것부터, 같은 계층이면 법정동코드 순.
     * 다음 페이지 판단을 위해 [Pagination.limit] 만큼 조회한다.
     */
    fun findByNameOrPathPrefix(namePrefix: String, pathPrefix: String?, pagination: Pagination): List<LegalDong>

    fun countByNameOrPathPrefix(namePrefix: String, pathPrefix: String?): Long

    /** 전체 경로가 fullName 인 시군구 이하 법정동. 자동완성 응답의 지역 keyword 를 되돌린다 */
    fun findByFullName(fullName: String): LegalDong?

    /** 서울 자치구 25개. NER 사전의 지역 원천 */
    fun findSeoulDistricts(): List<LegalDong>
}
