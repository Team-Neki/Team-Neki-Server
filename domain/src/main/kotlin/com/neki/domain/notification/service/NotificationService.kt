package com.neki.domain.notification.service

import com.neki.domain.notification.dto.NotificationCommand
import com.neki.domain.notification.dto.NotificationQuery
import com.neki.domain.notification.models.Notification
import com.neki.domain.notification.models.NotificationHist
import com.neki.domain.notification.repository.NotificationHistRepository
import com.neki.domain.notification.repository.NotificationRepository
import org.springframework.stereotype.Component

/**
 * fileName       : NotificationService
 * author         : koo
 * date           : 2026. 8. 4.
 * description    : 알림 도메인 서비스
 */
@Component
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val notificationHistRepository: NotificationHistRepository,
) {

    /**
     * 알림 정보가 등록되지 않은 사용자는 미동의로 본다.
     */
    fun isPushAgreed(query: NotificationQuery.GetPushAgreement): Boolean =
        notificationRepository.findByUserId(query.userId)?.pushAgreed ?: false

    /**
     * 사용자당 알림 정보는 한 건만 유지한다. 기존 정보가 있으면 갱신하고 없으면 생성한다.
     */
    fun saveOrUpdate(command: NotificationCommand.UpdateNotification): Notification {
        val existing: Notification? = notificationRepository.findByUserId(command.userId)

        val notification: Notification = existing
            ?.apply { updateDevice(command.deviceToken, command.pushAgreed) }
            ?: Notification(
                userId = command.userId,
                deviceToken = command.deviceToken,
                pushAgreed = command.pushAgreed,
            )

        return notificationRepository.save(notification)
    }

    fun delete(command: NotificationCommand.DeleteNotification) = notificationRepository.deleteByUserId(command.userId)

    fun getRecentHists(query: NotificationQuery.GetRecentNotifications): List<NotificationHist> =
        notificationHistRepository.findRecentByUserId(query.userId)

    /**
     * 발송에 성공한 알림만 내역으로 남긴다 (최근 알림 조회용).
     */
    fun recordSentPush(command: NotificationCommand.SendPush): NotificationHist = notificationHistRepository.save(
        NotificationHist(
            userId = command.userId,
            type = command.type,
            title = command.title,
            body = command.body,
            link = command.link,
        ),
    )
}
