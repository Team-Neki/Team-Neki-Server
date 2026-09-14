package com.neki.api.pose.application

import com.neki.core.annotation.UseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.core.transaction.TransactionRunner
import com.neki.domain.pose.client.MediaClient
import com.neki.domain.pose.dto.PoseCommand
import com.neki.domain.pose.models.MediaAvailabilities
import com.neki.domain.pose.models.Pose
import com.neki.domain.pose.service.PoseService

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
        // apps:api 업로드는 인증된 사용자만 호출한다. userId가 없는 업로드는 apps:admin 전용이다.
        val userId: Long = requireNotNull(command.userId)

        val poses: List<Pose> = poseService.createPoses(command)

        val mediaIds: List<Long> = command.uploads.map { it.mediaId }
        verifyUploadedOrRollback(userId, mediaIds)

        try {
            transactionRunner.run { poseService.saveAll(poses) }
        } catch (e: Exception) {
            mediaClient.rollbackMediasUploaded(userId, mediaIds)
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
