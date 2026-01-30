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
        val isAvailable: MediaAvailability = mediaClient.verifyMediaUploaded(
            ownerId = command.userId,
            mediaId = command.mediaId,
        )

        if (isAvailable != MediaAvailability.AVAILABLE) {
            throw BusinessException(ResultCode.NOT_FOUND)
        }

        try {
            transactionRunner.run {
                val user: User = userRepository.findById(command.userId)
                    ?: throw BusinessException(ResultCode.NOT_FOUND_USER)
                user.updateProfileImage(command.mediaId)
            }
        } catch (e: Exception) {
            mediaClient.rollbackMediasUploaded(command.userId, listOf(command.mediaId))
            throw e
        }
    }
}
