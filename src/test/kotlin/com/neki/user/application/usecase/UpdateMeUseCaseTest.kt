package com.neki.user.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.testfixture.aUser
import com.neki.user.application.dto.UserCommand
import com.neki.user.application.port.UserRepositoryPort
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateMeUseCaseTest {

    lateinit var userRepository: UserRepositoryPort
    lateinit var useCase: UpdateMeUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        useCase = UpdateMeUseCase(userRepository)
    }

    @Test
    @DisplayName("정상 수정 - 유저 존재 시 이름 업데이트 및 변경 확인")
    fun `정상 수정 - 유저 존재 시 이름 업데이트 및 변경 확인`() {
        // Given
        val user = aUser(id = 1L, name = "기존이름")
        every { userRepository.findById(1L) } returns user

        // When
        useCase.execute(UserCommand.UpdateUserInfo(userId = 1L, name = "새이름"))

        // Then
        user.name shouldBe "새이름"
        verify(exactly = 1) { userRepository.findById(1L) }
    }

    @Test
    @DisplayName("미존재 유저 수정 시 NOT_FOUND_USER BusinessException 발생")
    fun `미존재 유저 수정 시 NOT_FOUND_USER BusinessException 발생`() {
        // Given
        every { userRepository.findById(999L) } returns null

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(UserCommand.UpdateUserInfo(userId = 999L, name = "새이름"))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND_USER
    }
}
