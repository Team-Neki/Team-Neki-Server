package com.neki.user.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.user.application.command.LogoutCommand
import com.neki.user.application.port.NotificationClientPort

/**
 * fileName       : LogoutUseCase
 * author         : darren
 * date           : 2026. 6. 20
 * description    : 로그아웃 usecase - 사용자의 FCM 토큰을 삭제하여 더 이상 푸시가 전송되지 않도록 한다
 */
@UseCase
class LogoutUseCase(private val notificationClient: NotificationClientPort) {

    fun execute(command: LogoutCommand) {
        notificationClient.deleteFcmToken(command.userId)
    }
}
