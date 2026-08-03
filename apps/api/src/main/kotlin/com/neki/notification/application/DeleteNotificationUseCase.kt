package com.neki.notification.application

import com.neki.common.annotation.UseCase
import com.neki.notification.dto.NotificationCommand
import com.neki.notification.service.NotificationService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : DeleteNotificationUseCase
 * author         : darren
 * date           : 2026. 6. 20
 * description    : 사용자의 FCM 토큰(알림 정보) 삭제 (미등록 시 no-op)
 */
@UseCase
class DeleteNotificationUseCase(private val notificationService: NotificationService) {

    @Transactional
    fun execute(command: NotificationCommand.DeleteNotification) = notificationService.delete(command)
}
