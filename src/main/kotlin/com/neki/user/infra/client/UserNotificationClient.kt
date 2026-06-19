package com.neki.user.infra.client

import com.neki.notification.application.command.DeleteNotificationCommand
import com.neki.notification.application.command.GetPushAgreementCommand
import com.neki.notification.application.result.GetPushAgreementResult
import com.neki.notification.application.usecase.DeleteNotificationUseCase
import com.neki.notification.application.usecase.GetPushAgreementUseCase
import com.neki.user.application.port.NotificationClientPort
import org.springframework.stereotype.Component

@Component
class UserNotificationClient(
    private val getPushAgreementUseCase: GetPushAgreementUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
) : NotificationClientPort {

    override fun isPushAgreed(userId: Long): Boolean {
        val result: GetPushAgreementResult = getPushAgreementUseCase.execute(
            GetPushAgreementCommand(userId = userId),
        )
        return result.pushAgreed
    }

    override fun deleteFcmToken(userId: Long) = deleteNotificationUseCase.execute(
        DeleteNotificationCommand(userId = userId),
    )
}
