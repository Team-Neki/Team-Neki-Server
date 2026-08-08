package com.neki.api.user.application

import com.neki.core.annotation.UseCase
import com.neki.domain.user.dto.UserCommand
import com.neki.domain.user.service.UserService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateUserUseCase
 * author         : koo
 * date           : 2026. 1. 28. 오후 3:47
 * description    : 프로필 이미지를 제외한 사용자 정보 갱신 usecase
 */
@UseCase
class UpdateMeUseCase(private val userService: UserService) {

    @Transactional
    fun execute(command: UserCommand.UpdateUserInfo) {
        userService.updateUserInfo(command)
    }
}
