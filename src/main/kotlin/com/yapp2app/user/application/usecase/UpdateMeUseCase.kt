package com.yapp2app.user.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.user.application.command.UpdateUserCommand
import com.yapp2app.user.application.contract.MediaAvailability
import com.yapp2app.user.application.port.MediaClientPort
import com.yapp2app.user.application.port.UserRepositoryPort

/**
 * fileName       : UpdateUserUseCase
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:47
 * description    :
 */
@UseCase
class UpdateMeUseCase(
    private val userRepository: UserRepositoryPort,
    private val mediaClient: MediaClientPort,
    private val transactionRunner: TransactionRunner,
) {
    fun execute(command: UpdateUserCommand) {
        val verifiedMediaId: Long? = command.mediaId
        if (verifiedMediaId != null) {
            val result = mediaClient.verifyMediasUploaded(
                ownerId = command.userId,
                mediaIds = listOf(verifiedMediaId),
            )

            if (result[verifiedMediaId] != MediaAvailability.AVAILABLE) {
                mediaClient.rollbackMediasUploaded(command.userId, listOf(verifiedMediaId))
                throw BusinessException(ResultCode.UPLOAD_FAILED)
            }
        }

        try {
            transactionRunner.run {
                val me = userRepository.findById(command.userId)
                    ?: throw BusinessException(ResultCode.NOT_FOUND)
                me.updateInfo(command.name, verifiedMediaId)
            }
        } catch (e: Exception) {
            if (verifiedMediaId != null) {
                mediaClient.rollbackMediasUploaded(command.userId, listOf(verifiedMediaId))
            }
            throw e
        }
    }
}
