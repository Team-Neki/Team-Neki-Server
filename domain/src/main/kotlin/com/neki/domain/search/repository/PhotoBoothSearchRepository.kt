package com.neki.domain.search.repository

import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothSearchWrite
import com.neki.domain.search.models.SubwayStation

/**
 * fileName       : PhotoBoothSearchRepository
 * author         : koo
 * date           : 2026. 9. 25.
 * description    : 검색 카드 저장소 포트. 색인 입력(enriched, 역)을 읽고, _write 를 채운 뒤 _read 와 맞바꾼다
 */
interface PhotoBoothSearchRepository {

    fun findAllEnriched(): List<PhotoBoothEnriched>

    fun findAllStations(): List<SubwayStation>

    /** 지금 검색 API 가 읽고 있는(_read) 카드 수 */
    fun countCurrent(): Long

    /** _write 두 테이블을 비우고 cards 를 넣은 뒤 통계를 갱신한다. _read 는 건드리지 않는다. 호출자의 트랜잭션 안에서 돈다 */
    fun replaceWrite(cards: List<PhotoBoothSearchWrite>)

    /** _read 와 _write 의 이름을 연결 테이블까지 맞바꾼다. 호출자의 트랜잭션 안에서 돌고, 락을 짧게만 기다린다 */
    fun swap()
}
