package com.neki.api.user.infra.client

import com.neki.api.notification.application.DeleteNotificationUseCase
import com.neki.api.notification.application.GetPushAgreementUseCase
import com.neki.api.notification.application.dto.NotificationResult
import com.neki.domain.notification.dto.NotificationCommand
import com.neki.domain.notification.dto.NotificationQuery
import com.neki.domain.user.client.NotificationClient
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
