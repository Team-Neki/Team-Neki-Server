package com.yapp2app.pose.application.port

import com.yapp2app.common.domain.vo.SortOrder
import com.yapp2app.pose.domain.entity.Pose

/**
 * fileName       : PoseRepositoryPort
 * author         : darren
 * date           : 2026. 1. 27. 17:12
 * description    :
 */
interface PoseRepositoryPort {

    fun saveAll(poses: List<Pose>): List<Pose>

    fun listPoses(offset: Int, limit: Int, sortOrder: SortOrder): List<Pose>
}
