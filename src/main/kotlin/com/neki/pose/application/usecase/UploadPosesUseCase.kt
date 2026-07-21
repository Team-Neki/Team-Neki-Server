package com.neki.pose.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.pose.application.contract.MediaAvailability
import com.neki.pose.application.dto.PoseCommand
import com.neki.pose.application.port.MediaClientPort
import com.neki.pose.application.port.PoseRepositoryPort
import com.neki.pose.domain.entity.Pose

/**
 * fileName       : UploadPosesUseCase
 * author         : darren
 * date           : 2026. 1. 27. 17:14
 * description    :
 */
@UseCase
class UploadPosesUseCase(
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
    private val poseRepository: PoseRepositoryPort,
) {
    fun execute(command: PoseCommand.UploadPoses) {
        validateNoDuplicateMediaIds(command.uploads)

        val mediaIds: List<Long> = command.uploads.map { it.mediaId }

        // 모든 media가 object storage에 정상적으로 저장되었는지 일괄 확인
        val availabilities: Map<Long, MediaAvailability> = mediaClient.verifyMediasUploaded(
            ownerId = command.userId,
            mediaIds = mediaIds,
        )

        rollbackIfFailed(command.userId, availabilities)

        val poses = command.uploads.map { upload ->
            Pose(
                userId = command.userId,
                mediaId = upload.mediaId,
                headCount = upload.headCount,
                memo = upload.memo,
            )
        }

        try {
            transactionRunner.run {
                poseRepository.saveAll(poses)
            }
        } catch (e: Exception) {
            mediaClient.rollbackMediasUploaded(command.userId, mediaIds)
            throw e
        }
    }

    private fun validateNoDuplicateMediaIds(uploads: List<PoseCommand.UploadPoses.UploadItem>) {
        val mediaIds: List<Long> = uploads.map { it.mediaId }
        val duplicates: Set<Long> = mediaIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys

        if (duplicates.isNotEmpty()) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }

    private fun rollbackIfFailed(userId: Long, availabilities: Map<Long, MediaAvailability>) {
        val unavailableMediaIds: Set<Long> = availabilities
            .filter { it.value != MediaAvailability.AVAILABLE }
            .keys

        if (unavailableMediaIds.isNotEmpty()) {
            val successfulMediaIds = availabilities
                .filter { it.value == MediaAvailability.AVAILABLE }
                .keys
                .toList()

            if (successfulMediaIds.isNotEmpty()) {
                mediaClient.rollbackMediasUploaded(userId, successfulMediaIds)
            }

            throw BusinessException(ResultCode.UPLOAD_FAILED)
        }
    }
}
