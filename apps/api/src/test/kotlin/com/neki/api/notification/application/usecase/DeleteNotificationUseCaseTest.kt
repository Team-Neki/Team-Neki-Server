package com.neki.api.notification.application.usecase

import com.neki.api.notification.application.DeleteNotificationUseCase
import com.neki.domain.notification.dto.NotificationCommand
import com.neki.domain.notification.repository.NotificationRepository
import com.neki.domain.notification.service.NotificationService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DeleteNotificationUseCaseTest {

    lateinit var notificationRepository: NotificationRepository
    lateinit var useCase: DeleteNotificationUseCase

    @BeforeEach
    fun setUp() {
        notificationRepository = mockk()
        useCase = DeleteNotificationUseCase(NotificationService(notificationRepository, mockk()))
    }

    @Test
    @DisplayName("userId로 알림 정보를 삭제한다")
    fun `userId로 알림 정보를 삭제한다`() {
        // Given
        val userId = 1L
        every { notificationRepository.deleteByUserId(userId) } just Runs

        // When
        useCase.execute(NotificationCommand.DeleteNotification(userId = userId))

        // Then
        verify(exactly = 1) { notificationRepository.deleteByUserId(userId) }
    }
}
