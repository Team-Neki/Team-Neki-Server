package com.neki.core.domain.vo

/**
 * fileName       : SortOrder
 * author         : koo
 * date           : 2026. 1. 14.
 * description    : 범용 정렬 순서 - 여러 도메인에서 재사용 가능
 */
enum class SortOrder {
    ASC, // 오래된순 (과거 → 현재)
    DESC, // 최신순 (현재 → 과거)
    ;

    companion object {
        val DEFAULT = DESC
    }
}
