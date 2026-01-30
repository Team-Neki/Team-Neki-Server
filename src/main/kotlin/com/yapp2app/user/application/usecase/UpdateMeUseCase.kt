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
        if (command.name == null && command.mediaId == null) return

        // 프로필 이미지 변경이 포함된 경우
        var verifiedMediaId: Long? = command.mediaId
        if (verifiedMediaId != null) {
            // 미디어 업로드 검증
            val result = mediaClient.verifyMediasUploaded(
                ownerId = command.userId,
                mediaIds = listOf(verifiedMediaId),
            )

            // 이미지 업로드 실패 시 롤백 처리
            if (result[verifiedMediaId] != MediaAvailability.AVAILABLE) {
                mediaClient.rollbackMediasUploaded(command.userId, listOf(verifiedMediaId))

                // 이름 변경도 없으면 더 이상 할 일 없음
                if (command.name == null) {
                    throw BusinessException(ResultCode.UPLOAD_FAILED)
                }
                // 프로필 이미지 변경에 실패한 경우 이름 변경은 계속 진행
                verifiedMediaId = null
            }
        }

        try {
            transactionRunner.run {
                val me = userRepository.findById(command.userId)
                    ?: throw BusinessException(ResultCode.NOT_FOUND)
                me.updateInfo(command.name, verifiedMediaId)
            }
        } catch (e: Exception) {
            // 보상 트랜잭션: 검증된 media 롤백
            if (verifiedMediaId != null) {
                mediaClient.rollbackMediasUploaded(command.userId, listOf(verifiedMediaId))
            }
            throw e
        }
    }
}
