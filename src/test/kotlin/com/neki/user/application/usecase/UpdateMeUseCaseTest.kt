package com.neki.user.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.testfixture.aUser
import com.neki.user.application.command.UpdateUserInfoCommand
import com.neki.user.application.port.UserRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UpdateMeUseCaseTest :
    FunSpec({

        lateinit var userRepository: UserRepositoryPort
        lateinit var useCase: UpdateMeUseCase

        beforeTest {
            userRepository = mockk()
            useCase = UpdateMeUseCase(userRepository)
        }

        test("정상 수정 - 유저 존재 시 이름 업데이트 및 변경 확인") {
            // Given
            val user = aUser(id = 1L, name = "기존이름")
            every { userRepository.findById(1L) } returns user

            // When
            useCase.execute(UpdateUserInfoCommand(userId = 1L, name = "새이름"))

            // Then
            user.name shouldBe "새이름"
            verify(exactly = 1) { userRepository.findById(1L) }
        }

        test("미존재 유저 수정 시 NOT_FOUND_USER BusinessException 발생") {
            // Given
            every { userRepository.findById(999L) } returns null

            // When & Then
            val exception = shouldThrow<BusinessException> {
                useCase.execute(UpdateUserInfoCommand(userId = 999L, name = "새이름"))
            }
            exception.resultCode shouldBe ResultCode.NOT_FOUND_USER
        }
    })
