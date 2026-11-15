package com.neki.domain.pose.models

/**
 * fileName       : PoseSort
 * author         : darren
 * date           : 2026. 11. 15.
 * description    : 포즈 목록 정렬 기준. 기준마다 방향과 동점 처리가 정해져 있어 방향(SortOrder)은 따로 받지 않는다.
 *                  인기순(행동 로그 집계 점수)은 배치 적재가 생기면 값과 분기를 추가한다.
 */
enum class PoseSort {
    /** created_at DESC, id DESC */
    LATEST,
}
