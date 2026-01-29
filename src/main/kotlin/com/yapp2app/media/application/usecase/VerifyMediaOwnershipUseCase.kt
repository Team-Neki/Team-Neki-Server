package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.media.application.command.VerifyMediaOwnershipCommand
import com.yapp2app.media.application.port.MediaRepositoryPort

@UseCase
class VerifyMediaOwnershipUseCase(private val mediaRepository: MediaRepositoryPort) {

    fun execute(command: VerifyMediaOwnershipCommand) {
        mediaRepository.getActiveMedia(command.ownerId, command.mediaId)
            ?: throw BusinessException(ResultCode.NOT_FOUND)
    }
}
