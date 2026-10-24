package com.neki.core.domain.vo

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException

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

    init {
        // page 는 0부터, size 는 1부터. 그리고 page * size 가 Int 를 넘으면 offset 이 음수가 되어
        // 조회가 깨진다. 그만큼 뒤에는 데이터도 없으므로 잘못된 요청으로 본다.
        if (page < 0 || size < 1 || page.toLong() * size > Int.MAX_VALUE) {
            throw BusinessException(ResultCode.INVALID_PAGINATION)
        }
    }

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

    /**
     * [slice] 결과에 따로 센 전체 건수를 붙인다.
     */
    fun <T> slice(fetched: List<T>, totalCount: Long): PageWithTotalCount<T> {
        val page: Page<T> = slice(fetched)
        return PageWithTotalCount(page.items, page.hasNext, totalCount)
    }
}

/**
 * 다음 페이지 존재 여부를 함께 담은 조회 결과
 */
data class Page<T>(val items: List<T>, val hasNext: Boolean)

/**
 * 다음 페이지 존재 여부와 전체 건수를 함께 담은 조회 결과. 무한 스크롤이면서 건수 배지를 다는 화면에서 쓴다.
 * 총 페이지 수가 필요하면 [CountedPage] 를 쓴다.
 */
data class PageWithTotalCount<T>(val items: List<T>, val hasNext: Boolean, val totalCount: Long)

/**
 * 전체 건수를 함께 담은 조회 결과. 총 페이지 수를 노출해야 하는 목록 화면에서 쓴다.
 * hasNext 만 필요하면 [Page] 를 쓴다.
 */
data class CountedPage<T>(val items: List<T>, val totalCount: Long, val size: Int) {

    val totalPages: Int
        get() = if (size <= 0) 0 else ((totalCount + size - 1) / size).toInt()
}
