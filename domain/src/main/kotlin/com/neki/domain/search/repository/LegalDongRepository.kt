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
     * 이름이 prefix 로 시작하는 시군구 이하 법정동. 계층이 위인 것부터, 같은 계층이면 법정동코드 순.
     * 다음 페이지 판단을 위해 [Pagination.limit] 만큼 조회한다.
     */
    fun findByLeafNamePrefix(prefix: String, pagination: Pagination): List<LegalDong>

    fun countByLeafNamePrefix(prefix: String): Long
}
