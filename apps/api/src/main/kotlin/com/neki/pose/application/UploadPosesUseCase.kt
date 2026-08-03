package com.neki.pose.application

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.pose.MediaClient
import com.neki.pose.dto.PoseCommand
import com.neki.pose.models.MediaAvailabilities
import com.neki.pose.models.Pose
import com.neki.pose.service.PoseService

/**
 * fileName       : UploadPosesUseCase
 * author         : darren
 * date           : 2026. 1. 27. 17:14
 * description    :
 */
@UseCase
class UploadPosesUseCase(
    private val poseService: PoseService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: PoseCommand.UploadPoses) {
        val poses: List<Pose> = poseService.createPoses(command)

        val mediaIds: List<Long> = command.uploads.map { it.mediaId }
        verifyUploadedOrRollback(command.userId, mediaIds)

        try {
            transactionRunner.run { poseService.saveAll(poses) }
        } catch (e: Exception) {
            mediaClient.rollbackMediasUploaded(command.userId, mediaIds)
            throw e
        }
    }

    /**
     * media가 하나라도 스토리지에 없으면 포즈를 만들지 않고, 이미 올라간 media는 되돌린다.
     */
    private fun verifyUploadedOrRollback(userId: Long, mediaIds: List<Long>) {
        val availabilities = MediaAvailabilities(
            mediaClient.verifyMediasUploaded(ownerId = userId, mediaIds = mediaIds),
        )

        if (!availabilities.hasUnavailable) return

        val uploadedMediaIds: List<Long> = availabilities.availableMediaIds
        if (uploadedMediaIds.isNotEmpty()) {
            mediaClient.rollbackMediasUploaded(userId, uploadedMediaIds)
        }

        throw BusinessException(ResultCode.UPLOAD_FAILED)
    }
}
