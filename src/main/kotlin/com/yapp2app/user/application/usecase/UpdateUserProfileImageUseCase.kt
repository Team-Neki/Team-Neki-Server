package com.yapp2app.user.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.user.application.command.UpdateUserProfileImageCommand
import com.yapp2app.user.application.contract.MediaAvailability
import com.yapp2app.user.application.port.MediaClientPort
import com.yapp2app.user.application.port.UserRepositoryPort
import com.yapp2app.user.domain.entity.User

/**
 * fileName       : UpdateUserProfileUseCase
 * author         : koo
 * date           : 2026. 1. 31. 오전 12:01
 * description    : 사용자 프로필 이미지 변경 usecase
 */
@UseCase
class UpdateUserProfileImageUseCase(
    private val userRepository: UserRepositoryPort,
    private val mediaClient: MediaClientPort,

    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: UpdateUserProfileImageCommand) {
        val newMediaId = command.mediaId

        if (newMediaId != null) {
            verifyAndUpdateProfileImage(command.userId, newMediaId)
        } else {
            // null일 경우 기본 이미지로 변경
            updateProfileImageToDefault(command.userId)
        }
    }

    /**
     * mediaId가 null이 아닌 경우: media 업로드 검증 후 프로필 이미지 변경
     */
    private fun verifyAndUpdateProfileImage(userId: Long, newMediaId: Long) {
        val isAvailable: MediaAvailability = mediaClient.verifyMediaUploaded(
            ownerId = userId,
            mediaId = newMediaId,
        )

        if (isAvailable != MediaAvailability.AVAILABLE) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        try {
            transactionRunner.run {
                val user: User = userRepository.findById(userId)
                    ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

                val oldMediaId = user.profileImageId

                user.updateProfileImage(newMediaId)

                oldMediaId?.let { mediaClient.deleteMedia(userId, it) }
            }
        } catch (e: Exception) {
            mediaClient.rollbackMediasUploaded(userId, listOf(newMediaId))
            throw e
        }
    }

    /**
     * mediaId가 null인 경우: 기본 이미지로 변경 (profileImageId = null)
     */
    private fun updateProfileImageToDefault(userId: Long) {
        transactionRunner.run {
            val user: User = userRepository.findById(userId)
                ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

            val oldMediaId = user.profileImageId

            user.updateProfileImage(null)

            oldMediaId?.let { mediaClient.deleteMedia(userId, it) }
        }
    }
}
