package com.neki.user.infra.client

import com.neki.notification.application.dto.NotificationCommand
import com.neki.notification.application.dto.NotificationQuery
import com.neki.notification.application.dto.NotificationResult
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
        val result: NotificationResult.GetPushAgreement = getPushAgreementUseCase.execute(
            NotificationQuery.GetPushAgreement(userId = userId),
        )
        return result.pushAgreed
    }

    override fun deleteFcmToken(userId: Long) = deleteNotificationUseCase.execute(
        NotificationCommand.DeleteNotification(userId = userId),
    )
}
