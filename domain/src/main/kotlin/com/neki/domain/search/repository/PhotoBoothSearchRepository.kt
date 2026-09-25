package com.neki.domain.search.repository

import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.SubwayStation

/**
 * fileName       : PhotoBoothSearchRepository
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 카드 저장소 포트. 색인 입력(enriched, 역)을 읽고 카드를 전량 교체한다
 */
interface PhotoBoothSearchRepository {

    fun findAllEnriched(): List<PhotoBoothEnriched>

    fun findAllStations(): List<SubwayStation>

    fun count(): Long

    /** 연결 테이블, 카드 순으로 비우고 cards 를 넣는다. 호출자의 트랜잭션 안에서 돈다 */
    fun replaceAll(cards: List<PhotoBoothSearch>)
}
