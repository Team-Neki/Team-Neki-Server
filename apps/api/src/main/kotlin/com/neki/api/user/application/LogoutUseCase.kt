package com.neki.api.user.application

import com.neki.core.annotation.UseCase
import com.neki.domain.user.client.NotificationClient
import com.neki.domain.user.dto.UserCommand

/**
 * fileName       : LogoutUseCase
 * author         : darren
 * date           : 2026. 6. 20
 * description    : 로그아웃 usecase - 사용자의 FCM 토큰을 삭제하여 더 이상 푸시가 전송되지 않도록 한다
 */
@UseCase
class LogoutUseCase(private val notificationClient: NotificationClient) {

    fun execute(command: UserCommand.Logout) {
        notificationClient.deleteFcmToken(command.userId)
    }
}
