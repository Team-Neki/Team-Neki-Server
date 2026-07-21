package com.neki.notification.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.notification.application.command.SendPushCommand
import com.neki.notification.application.port.NotificationHistRepositoryPort
import com.neki.notification.application.port.PushNotificationPort
import com.neki.notification.application.result.SendPushResult
import com.neki.notification.entity.NotificationHist

/**
 * fileName       : SendPushUseCase
 * author         : darren
 * date           : 2026. 6. 14
 * description    : 전달받은 디바이스 토큰으로 푸시 알림을 발송하고, 발송 내역을 저장한다.
 */
@UseCase
class SendPushUseCase(
    private val pushNotificationPort: PushNotificationPort,
    private val notificationHistRepository: NotificationHistRepositoryPort,
) {

    fun execute(command: SendPushCommand): SendPushResult {
        val messageId: String =
            pushNotificationPort.send(command.token, command.title, command.body, command.link)

        // 발송에 성공한 알림만 내역으로 저장한다 (최근 알림 조회용)
        notificationHistRepository.save(
            NotificationHist(
                userId = command.userId,
                type = command.type,
                title = command.title,
                body = command.body,
                link = command.link,
            ),
        )

        return SendPushResult(messageId = messageId)
    }
}
