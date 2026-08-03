package com.neki.pose.models

/**
 * fileName       : PoseWithScrap
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 포즈와 스크랩 여부를 함께 담은 조회 결과
 */
data class PoseWithScrap(val pose: Pose, val isScraped: Boolean)
