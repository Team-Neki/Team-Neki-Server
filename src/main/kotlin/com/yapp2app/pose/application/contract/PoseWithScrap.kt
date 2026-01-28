package com.yapp2app.pose.application.contract

import com.yapp2app.pose.domain.entity.Pose

/**
 * fileName       : PoseWithScrap
 * author         : darren
 * date           : 2026. 1. 28
 * description    :
 */
data class PoseWithScrap(val pose: Pose, val isScrap: Boolean)
