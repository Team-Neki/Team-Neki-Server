package com.neki.api.notification.application

import com.neki.api.notification.application.dto.NotificationResult
import com.neki.core.annotation.UseCase
import com.neki.domain.notification.dto.NotificationCommand
import com.neki.domain.notification.external.PushNotificationSender
import com.neki.domain.notification.service.NotificationService

/**
 * fileName       : SendPushUseCase
 * author         : darren
 * date           : 2026. 6. 14
 * description    : 전달받은 디바이스 토큰으로 푸시 알림을 발송하고, 발송 내역을 저장한다.
 */
@UseCase
class SendPushUseCase(
    private val pushNotificationSender: PushNotificationSender,
    private val notificationService: NotificationService,
) {

    fun execute(command: NotificationCommand.SendPush): NotificationResult.SendPush {
        val messageId: String =
            pushNotificationSender.send(command.token, command.title, command.body, command.link)

        notificationService.recordSentPush(command)

        return NotificationResult.SendPush(messageId = messageId)
    }
}
