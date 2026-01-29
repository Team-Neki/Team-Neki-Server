package com.yapp2app.user.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.application.command.GetUserCommand
import com.yapp2app.user.application.port.UserRepositoryPort
import com.yapp2app.user.application.result.GetUserResult
import com.yapp2app.user.domain.entity.User

/**
 * fileName       : GetMyInfoUseCase
 * author         : koo
 * date           : 2026. 1. 30. 오전 3:25
 * description    :
 */
@UseCase
class GetUserInfoUseCase(private val userRepository: UserRepositoryPort) {

    fun execute(command: GetUserCommand): GetUserResult {
        val user: User = userRepository.findById(command.userId)
            ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        return GetUserResult(
            userId = user.id!!,
            name = user.name!!,
            email = user.email,
            objectKey = user.profileImageId?.toString(),
            providerType = user.providerType,
        )
    }
}
