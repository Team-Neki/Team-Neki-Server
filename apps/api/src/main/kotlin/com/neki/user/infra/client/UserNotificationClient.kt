package com.neki.user.infra.client

import com.neki.notification.application.DeleteNotificationUseCase
import com.neki.notification.application.GetPushAgreementUseCase
import com.neki.notification.application.dto.NotificationResult
import com.neki.notification.dto.NotificationCommand
import com.neki.notification.dto.NotificationQuery
import com.neki.user.client.NotificationClient
import org.springframework.stereotype.Component

@Component
class UserNotificationClient(
    private val getPushAgreementUseCase: GetPushAgreementUseCase,
    private val deleteNotificationUseCase: DeleteNotificationUseCase,
) : NotificationClient {

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
