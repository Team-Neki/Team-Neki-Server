package com.neki.notification.application.usecase

import com.neki.notification.NotificationRepository
import com.neki.notification.application.GetPushAgreementUseCase
import com.neki.notification.dto.NotificationQuery
import com.neki.notification.models.Notification
import com.neki.notification.service.NotificationService
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class GetPushAgreementUseCaseTest {

    lateinit var notificationRepository: NotificationRepository
    lateinit var useCase: GetPushAgreementUseCase

    @BeforeEach
    fun setUp() {
        notificationRepository = mockk()
        useCase = GetPushAgreementUseCase(NotificationService(notificationRepository, mockk()))
    }

    @Test
    @DisplayName("등록된 사용자의 푸시 동의 여부를 반환한다")
    fun `등록된 사용자의 푸시 동의 여부를 반환한다`() {
        // Given
        val userId = 1L
        val notification = Notification(id = 10L, userId = userId, deviceToken = "token-123", pushAgreed = true)
        every { notificationRepository.findByUserId(userId) } returns notification

        // When
        val result = useCase.execute(NotificationQuery.GetPushAgreement(userId = userId))

        // Then
        result.pushAgreed shouldBe true
    }

    @Test
    @DisplayName("알림 미등록 사용자는 false를 반환한다")
    fun `알림 미등록 사용자는 false를 반환한다`() {
        // Given
        val userId = 1L
        every { notificationRepository.findByUserId(userId) } returns null

        // When
        val result = useCase.execute(NotificationQuery.GetPushAgreement(userId = userId))

        // Then
        result.pushAgreed shouldBe false
    }
}
