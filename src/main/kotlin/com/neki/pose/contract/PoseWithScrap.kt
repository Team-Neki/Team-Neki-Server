package com.neki.pose.contract

import com.neki.pose.domain.entity.Pose

/**
 * fileName       : PoseWithScrap
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
data class PoseWithScrap(val pose: Pose, val isScraped: Boolean)
