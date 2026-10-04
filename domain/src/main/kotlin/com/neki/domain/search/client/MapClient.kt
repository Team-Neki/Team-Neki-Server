package com.neki.domain.search.client

import com.neki.domain.search.models.MapBooth
import com.neki.domain.search.models.PhotoBoothSearch

/**
 * fileName       : MapClient
 * author         : koo
 * date           : 2026. 9. 30.
 * description    : map 도메인이 정본인 값을 받아오는 인터페이스. 지도 부스 id, 즐겨찾기, 사용자별 브랜드 순서
 */
interface MapClient {

    /**
     * 검색 색인 행의 원천 키 (platform, idx) 로 찾은 지도 부스. 관리자가 숨긴 지점과 지도에 아직 없는 행은 빠진다
     */
    fun findMapBooths(userId: Long, indexedBooths: List<PhotoBoothSearch>): List<MapBooth>

    /**
     * 사용자별 노출 순서로 정렬된 모든 브랜드 id
     */
    fun findOrderedBrandIds(userId: Long): List<Long>
}
