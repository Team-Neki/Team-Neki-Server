package com.neki.domain.pose.service

import com.neki.core.code.ResultCode
import com.neki.core.domain.vo.CountedPage
import com.neki.core.exception.BusinessException
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.dto.PoseQuery
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.models.PoseRelation
import com.neki.domain.pose.repository.PoseRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * fileName       : PoseService
 * author         : koo
 * date           : 2026. 8. 3.
 * description    : 포즈 자체의 목록 조회와 등록.
 *                  스크랩은 [PoseScrapService], 무작위 추첨은 [RandomPoseService] 가 맡는다
 */
@Component
class PoseService(private val poseRepository: PoseRepository) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    /**
     * 사용자별 스크랩 정보를 붙이지 않고 전체 건수를 함께 센다.
     */
    fun listAllPoses(query: PoseQuery.GetAllPoses): CountedPage<Pose> = CountedPage(
        items = poseRepository.findAll(query),
        totalCount = poseRepository.count(query),
        size = query.pagination.size,
    )

    /**
     * 한 번의 업로드에 같은 media 나 같은 sourceKey 를 두 번 담을 수 없다.
     * 관계는 2인 포즈에만 붙일 수 있다. 하나라도 어기면 요청 전체를 거부한다.
     */
    fun createPoses(command: PoseCommand.UploadPoses): List<Pose> {
        validateNoDuplicateMediaIds(command.uploads)
        validateRelations(command.uploads)
        validateNoDuplicateSourceKeys(command.uploads)

        return command.uploads.map { upload ->
            Pose(
                userId = command.userId,
                mediaId = upload.mediaId,
                headCount = upload.headCount,
                memo = upload.memo,
                relation = upload.relation,
                sourceKey = upload.sourceKey,
            )
        }
    }

    fun saveAll(poses: List<Pose>): List<Pose> = poseRepository.saveAll(poses)

    fun findExistingSourceKeys(sourceKeys: Collection<String>): Set<String> =
        poseRepository.findExistingSourceKeys(sourceKeys)

    /**
     * 이미 등록된 sourceKey 가 하나라도 있으면 요청 전체를 409 D-18 로 거부한다 (all-or-nothing).
     * 응답에는 키를 담지 않으므로 어떤 키가 겹쳤는지는 로그로 남긴다.
     */
    fun validateSourceKeysNotRegistered(command: PoseCommand.UploadPoses) {
        val existing: Set<String> = findExistingSourceKeys(command.uploads.mapNotNull { it.sourceKey })
        if (existing.isNotEmpty()) {
            log.warn("[POSE_SOURCE_KEY_CONFLICT] keys={}", existing)
            throw BusinessException(ResultCode.CONFLICT_POSE_SOURCE_KEY)
        }
    }

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

    private fun validateRelations(uploads: List<PoseCommand.UploadPoses.Item>) {
        val violated: Boolean = uploads.any { it.relation != null && !PoseRelation.isAllowedFor(it.headCount) }
        if (violated) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }

    private fun validateNoDuplicateSourceKeys(uploads: List<PoseCommand.UploadPoses.Item>) {
        val duplicates: Set<String> = uploads.mapNotNull { it.sourceKey }
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }
}
