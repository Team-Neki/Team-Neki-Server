package com.neki.notification.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.notification.application.command.GetPushAgreementCommand
import com.neki.notification.application.port.NotificationRepositoryPort
import com.neki.notification.application.result.GetPushAgreementResult

/**
 * fileName       : GetPushAgreementUseCase
 * author         : darren
 * date           : 2026. 6. 12
 * description    : 사용자의 푸시 알림 동의 여부 조회 (미등록 시 false)
 */
@UseCase
class GetPushAgreementUseCase(private val notificationRepository: NotificationRepositoryPort) {

    fun execute(command: GetPushAgreementCommand): GetPushAgreementResult {
        val pushAgreed: Boolean = notificationRepository.findByUserId(command.userId)?.pushAgreed ?: false

        return GetPushAgreementResult(pushAgreed = pushAgreed)
    }
}
