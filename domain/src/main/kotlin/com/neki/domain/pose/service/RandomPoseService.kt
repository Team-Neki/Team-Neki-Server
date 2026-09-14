package com.neki.domain.pose.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.external.RandomGenerator
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.repository.PoseRepository
import org.springframework.stereotype.Component

/**
 * fileName       : RandomPoseService
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : 무작위 포즈 추첨
 */
@Component
class RandomPoseService(private val poseRepository: PoseRepository, private val randomGenerator: RandomGenerator) {

    /**
     * 제외 대상을 뺀 모집단에서 무작위로 한 건을 고른다.
     */
    fun pickRandomPose(query: PoseQuery.GetRandomPose): Pose {
        val count: Long = poseRepository.countPoses(query.headCount, query.excludeIds)
        if (count == 0L) {
            throw BusinessException(ResultCode.NO_MORE_RANDOM_POSE)
        }

        val randomOffset: Long = randomGenerator.nextLong(count)

        return poseRepository.findPoseByOffset(randomOffset, query.headCount, query.excludeIds)
            ?: throw BusinessException(ResultCode.NO_MORE_RANDOM_POSE)
    }
}
