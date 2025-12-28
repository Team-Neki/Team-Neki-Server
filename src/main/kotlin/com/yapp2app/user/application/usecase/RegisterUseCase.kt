package com.yapp2app.user.application.usecase

import com.yapp2app.common.api.dto.ResultCode
import com.yapp2app.common.exception.BusinessException
import com.yapp2app.user.application.command.RegisterCommand
import com.yapp2app.user.application.repository.UserRepository
import com.yapp2app.user.domain.entity.User
import com.yapp2app.user.domain.enums.RoleType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : RegisterUseCase
 * author         : koo
 * date           : 2025. 12. 28. 오후 7:40
 * description    : 로컬 사용자 회원 가입을 위한 usecase
 */
@Service
class RegisterUseCase(private val userRepository: UserRepository, private val passwordEncoder: PasswordEncoder) {

    @Transactional
    fun execute(command: RegisterCommand) {
        if (userRepository.existsByEmailAndProviderType(command.email, command.providerType)) {
            throw BusinessException(ResultCode.ALREADY_SIGNUP)
        }

        val encodedPassword = passwordEncoder.encode(command.password)

        val user =
            User(
                email = command.email,
                name = command.name,
                roles = RoleType.USER.role,
                password = encodedPassword,
                providerType = command.providerType,
            )

        userRepository.save(user)
    }
}
