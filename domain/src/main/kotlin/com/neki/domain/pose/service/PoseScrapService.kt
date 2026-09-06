package com.neki.domain.pose.service

import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.Page
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseWithScrap
import com.neki.domain.pose.models.ScrapPose
import com.neki.domain.pose.repository.PoseRepository
import com.neki.domain.pose.repository.ScrapPoseRepository
import org.springframework.stereotype.Component

/**
 * fileName       : PoseScrapService
 * author         : koo
 * date           : 2026. 8. 10.
 * description    : 스크랩 상태 변경과, 스크랩 여부가 붙은 조회
 */
@Component
class PoseScrapService(
    private val poseRepository: PoseRepository,
    private val scrapPoseRepository: ScrapPoseRepository,
) {

    fun getOwnedPoseWithScrap(query: PoseQuery.GetPose): PoseWithScrap =
        poseRepository.getOwnedPoseWithScrap(query.userId, query.poseId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

    fun listPosesWithScrap(query: PoseQuery.GetPoses): Page<PoseWithScrap> = query.pagination.slice(
        poseRepository.listPosesWithScrap(
            userId = query.userId,
            offset = query.pagination.offset,
            limit = query.pagination.limit,
            headCount = query.headCount,
            sortOrder = query.pagination.sortOrder,
        ),
    )

    fun listOwnedScrapPoses(query: PoseQuery.GetScrapPoses): Page<Pose> = query.pagination.slice(
        poseRepository.listOwnedScrapPoses(
            userId = query.userId,
            offset = query.pagination.offset,
            limit = query.pagination.limit,
            sortOrder = query.pagination.sortOrder,
        ),
    )

    /**
     * 무작위로 고른 포즈는 query에 없으므로 함께 받는다.
     */
    fun isScraped(query: PoseQuery.GetRandomPose, pose: Pose): Boolean =
        scrapPoseRepository.existsOwnedPoseScrap(ScrapPose(query.userId, pose.id!!))

    fun updateScrap(command: PoseCommand.UpdatePoseScrap) {
        if (!poseRepository.existsPose(command.poseId)) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        val scrapPose = ScrapPose(command.userId, command.poseId)
        if (command.scrap) {
            scrapPoseRepository.add(scrapPose)
        } else {
            scrapPoseRepository.delete(scrapPose)
        }
    }
}
