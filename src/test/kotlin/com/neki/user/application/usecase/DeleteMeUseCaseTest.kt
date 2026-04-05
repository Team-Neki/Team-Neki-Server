package com.neki.user.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.testfixture.aUser
import com.neki.user.application.command.DeleteUserCommand
import com.neki.user.application.port.UserRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class DeleteMeUseCaseTest :
    FunSpec({

        lateinit var userRepository: UserRepositoryPort
        lateinit var useCase: DeleteMeUseCase

        beforeTest {
            userRepository = mockk()
            useCase = DeleteMeUseCase(userRepository)
        }

        test("정상 탈퇴 - 유저 존재 시 withdraw 호출 확인") {
            // Given
            val user = aUser(id = 1L, email = "test@example.com", oid = "some-oid")
            every { userRepository.findById(1L) } returns user

            // When
            useCase.execute(DeleteUserCommand(userId = 1L))

            // Then
            user.email shouldBe null
            user.oid shouldBe null
            verify(exactly = 1) { userRepository.findById(1L) }
        }

        test("미존재 유저 탈퇴 시 NOT_FOUND_USER BusinessException 발생") {
            // Given
            every { userRepository.findById(999L) } returns null

            // When & Then
            val exception = shouldThrow<BusinessException> {
                useCase.execute(DeleteUserCommand(userId = 999L))
            }
            exception.resultCode shouldBe ResultCode.NOT_FOUND_USER
        }
    })
