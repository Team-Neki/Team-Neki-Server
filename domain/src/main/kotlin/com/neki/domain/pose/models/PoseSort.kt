package com.neki.domain.pose.models

/**
 * fileName       : PoseSort
 * author         : darren
 * date           : 2026. 11. 15.
 * description    : 포즈 목록 정렬. 등록 시각이 같으면 포즈 ID 로 같은 방향 정렬한다.
 *                  인기순(행동 로그 집계 점수)은 배치 적재가 생기면 POPULAR 와 분기를 추가한다.
 */
enum class PoseSort(val description: String) {
    LATEST("최신순"),
    OLDEST("오래된 순"),
}
