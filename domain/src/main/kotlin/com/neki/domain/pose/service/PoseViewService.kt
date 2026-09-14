package com.neki.domain.pose.service

import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.external.PoseViewCache
import com.neki.domain.pose.repository.PoseRepository
import org.springframework.stereotype.Component

/**
 * fileName       : PoseViewService
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : 포즈 조회수 집계
 */
@Component
class PoseViewService(private val poseRepository: PoseRepository, private val poseViewCache: PoseViewCache) {

    /**
     * 같은 사용자의 재조회는 조회수에 반영하지 않는다.
     */
    fun isFirstViewOf(query: PoseQuery.GetPose): Boolean = poseViewCache.addViewer(query.poseId, query.userId)

    fun incrementViewCount(query: PoseQuery.GetPose) = poseRepository.incrementViewCount(query.poseId)
}
