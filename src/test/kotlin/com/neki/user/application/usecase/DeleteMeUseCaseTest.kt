package com.neki.user.application.usecase

import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.testfixture.aUser
import com.neki.user.application.command.DeleteUserCommand
import com.neki.user.application.port.UserRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DeleteMeUseCaseTest {

    lateinit var userRepository: UserRepositoryPort
    lateinit var useCase: DeleteMeUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        useCase = DeleteMeUseCase(userRepository)
    }

    @Test
    @DisplayName("정상 탈퇴 - 유저 존재 시 withdraw 호출 확인")
    fun `정상 탈퇴 - 유저 존재 시 withdraw 호출 확인`() {
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

    @Test
    @DisplayName("미존재 유저 탈퇴 시 NOT_FOUND_USER BusinessException 발생")
    fun `미존재 유저 탈퇴 시 NOT_FOUND_USER BusinessException 발생`() {
        // Given
        every { userRepository.findById(999L) } returns null

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(DeleteUserCommand(userId = 999L))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND_USER
    }
}
