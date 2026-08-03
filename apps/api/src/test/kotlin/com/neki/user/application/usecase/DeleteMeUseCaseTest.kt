package com.neki.user.application.usecase

import com.neki.common.code.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.testfixture.aUser
import com.neki.user.NotificationClient
import com.neki.user.TermClient
import com.neki.user.UserEventPublisher
import com.neki.user.UserRepository
import com.neki.user.application.DeleteMeUseCase
import com.neki.user.dto.UserCommand
import com.neki.user.service.UserService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DeleteMeUseCaseTest {

    lateinit var userRepository: UserRepository
    lateinit var userEventPublisher: UserEventPublisher
    lateinit var termClient: TermClient
    lateinit var notificationClient: NotificationClient
    lateinit var useCase: DeleteMeUseCase

    @BeforeEach
    fun setUp() {
        userRepository = mockk()
        userEventPublisher = mockk()
        termClient = mockk()
        notificationClient = mockk()
        useCase =
            DeleteMeUseCase(userEventPublisher, termClient, notificationClient, UserService(userRepository, mockk()))
    }

    @Test
    @DisplayName("정상 탈퇴 - 유저 존재 시 withdraw 호출 확인")
    fun `정상 탈퇴 - 유저 존재 시 withdraw 호출 확인`() {
        // Given
        val user = aUser(id = 1L, email = "test@example.com", oid = "some-oid")
        every { userRepository.findById(1L) } returns user
        every { userRepository.countByOidIsNotNull() } returns 1L
        every { userEventPublisher.publish(any()) } returns Unit
        every { termClient.revokeOptionalTerms(1L) } returns Unit
        every { notificationClient.deleteFcmToken(1L) } returns Unit

        // When
        useCase.execute(UserCommand.DeleteUser(userId = 1L))

        // Then
        user.email shouldBe null
        user.oid shouldBe null
        verify(exactly = 1) { userRepository.findById(1L) }
    }

    @Test
    @DisplayName("탈퇴 시 선택약관 철회가 호출된다")
    fun `탈퇴 시 선택약관 철회가 호출된다`() {
        // Given
        val user = aUser(id = 1L, email = "test@example.com", oid = "some-oid")
        every { userRepository.findById(1L) } returns user
        every { userRepository.countByOidIsNotNull() } returns 1L
        every { userEventPublisher.publish(any()) } returns Unit
        every { termClient.revokeOptionalTerms(1L) } returns Unit
        every { notificationClient.deleteFcmToken(1L) } returns Unit

        // When
        useCase.execute(UserCommand.DeleteUser(userId = 1L))

        // Then
        verify(exactly = 1) { termClient.revokeOptionalTerms(1L) }
    }

    @Test
    @DisplayName("탈퇴 시 FCM 토큰 삭제가 호출된다")
    fun `탈퇴 시 FCM 토큰 삭제가 호출된다`() {
        // Given
        val user = aUser(id = 1L, email = "test@example.com", oid = "some-oid")
        every { userRepository.findById(1L) } returns user
        every { userRepository.countByOidIsNotNull() } returns 1L
        every { userEventPublisher.publish(any()) } returns Unit
        every { termClient.revokeOptionalTerms(1L) } returns Unit
        every { notificationClient.deleteFcmToken(1L) } returns Unit

        // When
        useCase.execute(UserCommand.DeleteUser(userId = 1L))

        // Then
        verify(exactly = 1) { notificationClient.deleteFcmToken(1L) }
    }

    @Test
    @DisplayName("미존재 유저 탈퇴 시 NOT_FOUND_USER BusinessException 발생")
    fun `미존재 유저 탈퇴 시 NOT_FOUND_USER BusinessException 발생`() {
        // Given
        every { userRepository.findById(999L) } returns null

        // When & Then
        val exception = shouldThrow<BusinessException> {
            useCase.execute(UserCommand.DeleteUser(userId = 999L))
        }
        exception.resultCode shouldBe ResultCode.NOT_FOUND_USER
    }

    @Test
    @DisplayName("유저가 존재하지 않으면 선택약관 철회가 호출되지 않는다")
    fun `유저가 존재하지 않으면 선택약관 철회가 호출되지 않는다`() {
        // Given
        every { userRepository.findById(999L) } returns null

        // When & Then
        shouldThrow<BusinessException> {
            useCase.execute(UserCommand.DeleteUser(userId = 999L))
        }
        verify(exactly = 0) { termClient.revokeOptionalTerms(any()) }
        verify(exactly = 0) { notificationClient.deleteFcmToken(any()) }
    }
}
