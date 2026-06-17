package com.neki.pose.application.contract

import com.neki.pose.entity.Pose

/**
 * fileName       : PoseWithScrap
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
data class PoseWithScrap(val pose: Pose, val isScraped: Boolean)
