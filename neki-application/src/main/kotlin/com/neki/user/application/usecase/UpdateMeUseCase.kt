package com.neki.user.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.user.application.command.UpdateUserInfoCommand
import com.neki.user.application.port.UserRepositoryPort
import com.neki.user.entity.User
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateUserUseCase
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:47
 * description    : 프로필 이미지를 제외한 사용자 정보 갱신 usecase
 */
@UseCase
class UpdateMeUseCase(private val userRepository: UserRepositoryPort) {

    @Transactional
    fun execute(command: UpdateUserInfoCommand) {
        val user: User = (
            userRepository.findById(command.userId)
                ?: throw BusinessException(ResultCode.NOT_FOUND_USER)
            )

        user.updateName(command.name)
    }
}
