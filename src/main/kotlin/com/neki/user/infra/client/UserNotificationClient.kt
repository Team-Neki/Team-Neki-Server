package com.neki.user.infra.client

import com.neki.notification.application.command.GetPushAgreementCommand
import com.neki.notification.application.result.GetPushAgreementResult
import com.neki.notification.application.usecase.GetPushAgreementUseCase
import com.neki.user.application.port.NotificationClientPort
import org.springframework.stereotype.Component

@Component
class UserNotificationClient(private val getPushAgreementUseCase: GetPushAgreementUseCase) : NotificationClientPort {

    override fun isPushAgreed(userId: Long): Boolean {
        val result: GetPushAgreementResult = getPushAgreementUseCase.execute(
            GetPushAgreementCommand(userId = userId),
        )
        return result.pushAgreed
    }
}
