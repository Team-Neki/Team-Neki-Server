package com.neki.pose.service

import com.neki.common.code.ResultCode
import com.neki.common.domain.vo.Page
import com.neki.common.exception.BusinessException
import com.neki.pose.dto.PoseCommand
import com.neki.pose.dto.PoseQuery
import com.neki.pose.external.PoseViewCache
import com.neki.pose.external.RandomGenerator
import com.neki.pose.models.Pose
import com.neki.pose.models.PoseWithScrap
import com.neki.pose.models.ScrapPose
import com.neki.pose.repository.PoseRepository
import com.neki.pose.repository.ScrapPoseRepository
import org.springframework.stereotype.Component

/**
 * fileName       : PoseService
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : Pose 도메인 서비스
 */
@Component
class PoseService(
    private val poseRepository: PoseRepository,
    private val scrapPoseRepository: ScrapPoseRepository,
    private val poseViewCache: PoseViewCache,
    private val randomGenerator: RandomGenerator,
) {

    fun getOwnedPoseWithScrap(query: PoseQuery.GetPose): PoseWithScrap =
        poseRepository.getOwnedPoseWithScrap(query.userId, query.poseId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)

    /**
     * 같은 사용자의 재조회는 조회수에 반영하지 않는다.
     */
    fun isFirstViewOf(query: PoseQuery.GetPose): Boolean = poseViewCache.addViewer(query.poseId, query.userId)

    fun incrementViewCount(query: PoseQuery.GetPose) = poseRepository.incrementViewCount(query.poseId)

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

    /**
     * 한 번의 업로드에 같은 media를 두 번 담을 수 없다.
     */
    fun createPoses(command: PoseCommand.UploadPoses): List<Pose> {
        validateNoDuplicateMediaIds(command.uploads)

        return command.uploads.map { upload ->
            Pose(
                userId = command.userId,
                mediaId = upload.mediaId,
                headCount = upload.headCount,
                memo = upload.memo,
            )
        }
    }

    fun saveAll(poses: List<Pose>): List<Pose> = poseRepository.saveAll(poses)

    private fun validateNoDuplicateMediaIds(uploads: List<PoseCommand.UploadPoses.Item>) {
        val duplicates: Set<Long> = uploads.map { it.mediaId }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }
}
