package com.neki.notification.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.notification.application.command.UpdateNotificationCommand
import com.neki.notification.application.port.NotificationRepositoryPort
import com.neki.notification.domain.entity.Notification
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : UpdateNotificationUseCase
 * author         : darren
 * date           : 2026. 6. 12
 * description    : 알림 토큰 및 푸시 동의 여부 등록/수정 (upsert)
 */
@UseCase
class UpdateNotificationUseCase(private val notificationRepository: NotificationRepositoryPort) {

    @Transactional
    fun execute(command: UpdateNotificationCommand) {
        val existing: Notification? = notificationRepository.findByUserId(command.userId)

        val notification: Notification = existing?.apply {
            deviceToken = command.deviceToken
            pushAgreed = command.pushAgreed
        } ?: Notification(
            userId = command.userId,
            deviceToken = command.deviceToken,
            pushAgreed = command.pushAgreed,
        )

        notificationRepository.save(notification)
    }
}
