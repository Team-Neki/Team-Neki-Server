package com.neki.domain.pose.models

/**
 * fileName       : PoseSortType
 * author         : darren
 * date           : 2026. 11. 15.
 * description    : 포즈 목록 정렬 기준. 방향(오름·내림)은 공용 SortOrder 로 따로 받는다.
 *                  인기순(행동 로그 집계 점수)은 배치 적재가 생기면 POPULAR 와 분기를 추가한다.
 */
enum class PoseSortType {
    /** 등록 시각 (created_at, 같으면 id) */
    TIME,
}
