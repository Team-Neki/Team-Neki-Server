package com.neki.user.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.user.application.command.UpdateUserProfileImageCommand
import com.neki.user.application.contract.MediaAvailability
import com.neki.user.application.port.MediaClientPort
import com.neki.user.application.port.UserRepositoryPort
import com.neki.user.domain.entity.User

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

        var oldMediaId: Long? = null
        try {
            transactionRunner.run {
                val user: User = userRepository.findById(userId)
                    ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

                // 멱등성: 이미 동일한 이미지가 설정되어 있으면 변경하지 않음
                if (user.profileImageId == newMediaId) return@run

                oldMediaId = user.profileImageId
                user.updateProfileImage(newMediaId)
            }
        } catch (e: Exception) {
            mediaClient.rollbackMediasUploaded(userId, listOf(newMediaId))
            throw e
        }

        // 트랜잭션 외부에서 이전 이미지 삭제 (UnexpectedRollbackException 방지)
        oldMediaId?.let { mediaClient.deleteMedia(userId, it) }
    }

    /**
     * mediaId가 null인 경우: 기본 이미지로 변경 (profileImageId = null)
     */
    private fun updateProfileImageToDefault(userId: Long) {
        var oldMediaId: Long? = null
        transactionRunner.run {
            val user: User = userRepository.findById(userId)
                ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

            // 멱등성: 이미 기본 이미지인 경우 변경하지 않음
            if (user.profileImageId == null) return@run

            oldMediaId = user.profileImageId
            user.updateProfileImage(null)
        }

        // 트랜잭션 외부에서 이전 이미지 삭제
        oldMediaId?.let { mediaClient.deleteMedia(userId, it) }
    }
}
