package com.neki.domain.search.client

import com.neki.domain.search.models.SearchBrand

/**
 * fileName       : BrandClient
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : map 도메인의 브랜드를 읽는 인터페이스. 도메인 간 연결은 앱 모듈이 구현한다
 */
interface BrandClient {

    fun findAll(): List<SearchBrand>
}
