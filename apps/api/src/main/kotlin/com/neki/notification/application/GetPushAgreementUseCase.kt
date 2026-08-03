package com.neki.notification.application

import com.neki.common.annotation.UseCase
import com.neki.notification.application.dto.NotificationResult
import com.neki.notification.dto.NotificationQuery
import com.neki.notification.service.NotificationService

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
