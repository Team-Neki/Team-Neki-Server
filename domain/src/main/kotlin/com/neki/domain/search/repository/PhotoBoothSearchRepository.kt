package com.neki.domain.search.repository

import com.neki.core.domain.vo.Pagination
import com.neki.domain.search.models.PhotoBoothEnriched
import com.neki.domain.search.models.PhotoBoothSearch
import com.neki.domain.search.models.PhotoBoothSearchWrite
import com.neki.domain.search.models.SearchTarget
import com.neki.domain.search.models.SubwayStation
import com.neki.domain.search.models.UserLocation

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

    /** 검색 API 가 읽는(_read) 색인 행 중 범위(지역·역·지점)에 딸린 것. brandIds 가 null 이거나 비어 있으면 모든 브랜드 */
    fun findByScope(scope: SearchTarget.Scope, brandIds: List<Long>?): List<PhotoBoothSearch>

    /** 검색 API 가 읽는(_read) 색인 행 중 `브랜드명 지점명` 이 boothName 인 것. 부스 자동완성 keyword 를 지점으로 되돌린다 */
    fun findByBoothName(boothName: String): List<PhotoBoothSearch>

    /**
     * 검색 API 가 읽는(_read) 색인 행 중 검색어 전체가 이름 앞부분이거나 낱말(terms)마다 브랜드명·지점명·주소 중 하나에 들어 있는 것.
     * 부스 자동완성 한 페이지(limit 은 pagination.limit). 이름 앞부분 일치가 먼저 온다
     */
    fun findByKeyword(
        keyword: String,
        terms: List<List<String>>,
        userLocation: UserLocation?,
        pagination: Pagination,
    ): List<PhotoBoothSearch>

    fun countByKeyword(keyword: String, terms: List<List<String>>): Long

    /** 검색 API 가 읽는(_read) 색인에 있는 브랜드 id -> 이름. NER 사전의 브랜드 원천 */
    fun findIndexedBrandNames(): Map<Long, String>
}
