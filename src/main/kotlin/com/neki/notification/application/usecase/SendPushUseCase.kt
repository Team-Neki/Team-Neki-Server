package com.neki.notification.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.notification.application.command.SendPushCommand
import com.neki.notification.application.port.PushNotificationPort
import com.neki.notification.application.result.SendPushResult

/**
 * fileName       : SendPushUseCase
 * author         : darren
 * date           : 2026. 6. 14
 * description    : 전달받은 디바이스 토큰으로 푸시 알림을 발송한다.
 */
@UseCase
class SendPushUseCase(private val pushNotificationPort: PushNotificationPort) {

    fun execute(command: SendPushCommand): SendPushResult {
        val messageId: String =
            pushNotificationPort.send(command.token, command.title, command.body, command.link)

        return SendPushResult(messageId = messageId)
    }
}
