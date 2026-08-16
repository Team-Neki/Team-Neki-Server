package com.neki.domain.pose.service

import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.CountedPage
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.repository.PoseRepository
import org.springframework.stereotype.Component

/**
 * fileName       : PoseService
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : 포즈 자체의 목록 조회와 등록.
 *                  스크랩은 [PoseScrapService], 조회수는 [PoseViewService], 무작위 추첨은 [RandomPoseService] 가 맡는다
 */
@Component
class PoseService(private val poseRepository: PoseRepository) {

    /**
     * 사용자별 스크랩 정보를 붙이지 않고 전체 건수를 함께 센다.
     */
    fun listAllPoses(query: PoseQuery.GetAllPoses): CountedPage<Pose> = CountedPage(
        items = poseRepository.findAll(query),
        totalCount = poseRepository.count(query),
        size = query.pagination.size,
    )

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
