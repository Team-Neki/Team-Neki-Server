package com.neki.api.notification.application

import com.neki.api.notification.application.dto.NotificationResult
import com.neki.core.annotation.UseCase
import com.neki.domain.notification.dto.NotificationQuery
import com.neki.domain.notification.service.NotificationService

/**
 * fileName       : GetPushAgreementUseCase
 * author         : darren
 * date           : 2026. 6. 12
 * description    : 사용자의 푸시 알림 동의 여부 조회 (미등록 시 false)
 */
@UseCase
class GetPushAgreementUseCase(private val notificationService: NotificationService) {

    fun execute(query: NotificationQuery.GetPushAgreement): NotificationResult.GetPushAgreement =
        NotificationResult.GetPushAgreement(pushAgreed = notificationService.isPushAgreed(query))
}
