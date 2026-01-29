package com.yapp2app.pose.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.pose.application.contract.MediaAvailability
import com.yapp2app.pose.application.command.UploadPosesCommand
import com.yapp2app.pose.application.port.MediaClientPort
import com.yapp2app.pose.application.port.PoseRepositoryPort
import com.yapp2app.pose.domain.entity.Pose

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
    fun execute(command: UploadPosesCommand) {
        validateNoDuplicateMediaIds(command.uploads)

        val mediaIds = command.uploads.map { it.mediaId }

        // 모든 media가 object storage에 정상적으로 저장되었는지 일괄 확인
        val availabilities = mediaClient.verifyMediasUploaded(
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

    private fun validateNoDuplicateMediaIds(uploads: List<UploadPosesCommand.UploadItem>) {
        val mediaIds = uploads.map { it.mediaId }
        val duplicates = mediaIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys

        if (duplicates.isNotEmpty()) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
    }

    private fun rollbackIfFailed(userId: Long, availabilities: Map<Long, MediaAvailability>) {
        val unavailableMediaIds = availabilities
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
