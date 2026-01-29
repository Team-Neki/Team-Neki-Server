package com.yapp2app.user.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.user.application.command.UpdateUserCommand
import com.yapp2app.user.application.port.MediaClientPort
import com.yapp2app.user.application.port.UserRepositoryPort
import com.yapp2app.user.domain.entity.User

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
        // 둘 다 null인 경우 무시
        if (command.name == null && command.mediaId == null) {
            return
        }

        // 리소스 검증 & 소유권 검증
        if (command.mediaId != null) {
            mediaClient.verifyMediaOwned(
                ownerId = command.userId,
                mediaId = command.mediaId,
            )
        }

        // 트랜잭션 내에서 기존 이미지 ID 보존
        val oldProfileImageId: Long? = transactionRunner.run {
            val me: User = (
                userRepository.findById(command.userId)
                    ?: throw BusinessException(ResultCode.NOT_FOUND)
                )

            val previousImageId = me.profileImageId

            command.name?.let { me.changeName(it) }
            command.mediaId?.let { me.changeProfileImage(it) }

            // 실제로 이미지가 변경된 경우에만 반환
            if (command.mediaId != null && previousImageId != null && previousImageId != command.mediaId) {
                previousImageId
            } else {
                null
            }
        }

        // 트랜잭션 완료 후 기존 이미지 삭제 요청
        oldProfileImageId?.let {
            mediaClient.deleteMedia(command.userId, it)
        }
    }
}
