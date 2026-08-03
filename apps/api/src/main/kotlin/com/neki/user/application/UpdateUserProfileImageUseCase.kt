package com.neki.user.application

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.user.MediaClient
import com.neki.user.dto.UserCommand
import com.neki.user.models.MediaAvailability
import com.neki.user.service.UserService

/**
 * fileName       : UpdateUserProfileUseCase
 * author         : koo
 * date           : 2026. 1. 31. 오전 12:01
 * description    : 사용자 프로필 이미지 변경 usecase
 */
@UseCase
class UpdateUserProfileImageUseCase(
    private val userService: UserService,
    private val mediaClient: MediaClient,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: UserCommand.UpdateUserProfileImage) {
        val newMediaId: Long? = command.mediaId

        if (newMediaId != null) {
            verifyAndUpdateProfileImage(command, newMediaId)
        } else {
            // null일 경우 기본 이미지로 변경
            updateProfileImageToDefault(command)
        }
    }

    /**
     * mediaId가 null이 아닌 경우: media 업로드 검증 후 프로필 이미지 변경
     */
    private fun verifyAndUpdateProfileImage(command: UserCommand.UpdateUserProfileImage, newMediaId: Long) {
        val isAvailable: MediaAvailability = mediaClient.verifyMediaUploaded(
            ownerId = command.userId,
            mediaId = newMediaId,
        )

        if (isAvailable != MediaAvailability.AVAILABLE) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        val oldMediaId: Long? = try {
            transactionRunner.run { userService.updateProfileImage(command) }
        } catch (e: Exception) {
            mediaClient.rollbackMediasUploaded(command.userId, listOf(newMediaId))
            throw e
        }

        // 트랜잭션 외부에서 이전 이미지 삭제 (UnexpectedRollbackException 방지)
        oldMediaId?.let { mediaClient.deleteMedia(command.userId, it) }
    }

    /**
     * mediaId가 null인 경우: 기본 이미지로 변경 (profileImageId = null)
     */
    private fun updateProfileImageToDefault(command: UserCommand.UpdateUserProfileImage) {
        val oldMediaId: Long? = transactionRunner.run { userService.updateProfileImage(command) }

        // 트랜잭션 외부에서 이전 이미지 삭제 (UnexpectedRollbackException 방지)
        oldMediaId?.let { mediaClient.deleteMedia(command.userId, it) }
    }
}
