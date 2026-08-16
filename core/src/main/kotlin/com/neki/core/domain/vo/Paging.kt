package com.neki.core.domain.vo

/**
 * fileName       : Paging
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 페이지 조회 조건과 결과. 여러 도메인에서 재사용한다.
 */

/**
 * 페이지 조회 요청. offset/limit 계산과 조회 결과 슬라이싱을 함께 책임진다.
 */
data class Pagination(val page: Int, val size: Int, val sortOrder: SortOrder = SortOrder.DEFAULT) {

    val offset: Int
        get() = page * size

    /**
     * 다음 페이지 존재 여부를 알아내려고 한 건 더 조회한다.
     */
    val limit: Int
        get() = size + 1

    /**
     * limit으로 조회한 결과에서 초과분을 잘라내고 다음 페이지 존재 여부로 환산한다.
     */
    fun <T> slice(fetched: List<T>): Page<T> {
        val hasNext: Boolean = fetched.size > size
        return Page(if (hasNext) fetched.dropLast(1) else fetched, hasNext)
    }
}

/**
 * 다음 페이지 존재 여부를 함께 담은 조회 결과
 */
data class Page<T>(val items: List<T>, val hasNext: Boolean)

/**
 * 전체 건수를 함께 담은 조회 결과. 총 페이지 수를 노출해야 하는 목록 화면에서 쓴다.
 * hasNext 만 필요하면 [Page] 를 쓴다.
 */
data class CountedPage<T>(val items: List<T>, val totalCount: Long, val size: Int) {

    val totalPages: Int
        get() = if (size <= 0) 0 else ((totalCount + size - 1) / size).toInt()
}
