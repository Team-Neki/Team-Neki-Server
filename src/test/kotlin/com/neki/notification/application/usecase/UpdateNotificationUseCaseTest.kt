package com.neki.notification.application.usecase

import com.neki.notification.application.command.UpdateNotificationCommand
import com.neki.notification.application.port.NotificationRepositoryPort
import com.neki.notification.domain.entity.Notification
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class UpdateNotificationUseCaseTest {

    lateinit var notificationRepository: NotificationRepositoryPort
    lateinit var useCase: UpdateNotificationUseCase

    @BeforeEach
    fun setUp() {
        notificationRepository = mockk()
        useCase = UpdateNotificationUseCase(notificationRepository = notificationRepository)
    }

    @Test
    @DisplayName("기존 설정이 없으면 새 Notification을 생성하여 저장한다")
    fun `기존 설정이 없으면 새 Notification을 생성하여 저장한다`() {
        // Given
        val userId = 1L
        val command = UpdateNotificationCommand(userId = userId, deviceToken = "token-123", pushAgreed = true)

        val saved = slot<Notification>()
        every { notificationRepository.findByUserId(userId) } returns null
        every { notificationRepository.save(capture(saved)) } answers { saved.captured }

        // When
        useCase.execute(command)

        // Then
        saved.captured.userId shouldBe userId
        saved.captured.deviceToken shouldBe "token-123"
        saved.captured.pushAgreed shouldBe true
        verify(exactly = 1) { notificationRepository.save(any()) }
    }

    @Test
    @DisplayName("기존 설정이 있으면 기존 엔티티를 갱신하여 저장한다")
    fun `기존 설정이 있으면 기존 엔티티를 갱신하여 저장한다`() {
        // Given
        val userId = 1L
        val existing = Notification(id = 10L, userId = userId, deviceToken = "old-token", pushAgreed = false)
        val command = UpdateNotificationCommand(userId = userId, deviceToken = "new-token", pushAgreed = true)

        val saved = slot<Notification>()
        every { notificationRepository.findByUserId(userId) } returns existing
        every { notificationRepository.save(capture(saved)) } answers { saved.captured }

        // When
        useCase.execute(command)

        // Then
        saved.captured shouldBeSameInstanceAs existing
        saved.captured.deviceToken shouldBe "new-token"
        saved.captured.pushAgreed shouldBe true
        verify(exactly = 1) { notificationRepository.save(existing) }
    }
}
