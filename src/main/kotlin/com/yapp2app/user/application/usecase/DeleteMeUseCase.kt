package com.yapp2app.user.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.application.command.DeleteUserCommand
import com.yapp2app.user.application.port.UserRepositoryPort
import com.yapp2app.user.domain.entity.User
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : DeleteMeUseCase
 * author         : koo
 * date           : 2026. 1. 30. 오후 5:49
 * description    : 회원탍퇴 usecase
 * - 회원 탈퇴 시, 사용자 정보는 삭제하지 않고 상태만 '탈퇴'로 변경
 */
@UseCase
class DeleteMeUseCase(private val userRepository: UserRepositoryPort) {

    @Transactional
    fun execute(command: DeleteUserCommand) {
        val user: User = userRepository.findById(command.userId)
            ?: throw BusinessException(ResultCode.NOT_FOUND_USER)

        user.withdraw()
    }
}
