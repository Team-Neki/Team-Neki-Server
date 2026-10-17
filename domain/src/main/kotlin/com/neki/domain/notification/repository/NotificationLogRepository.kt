package com.neki.domain.notification.repository

import com.neki.domain.notification.models.NotificationLog
import com.neki.domain.notification.models.NotificationType
import java.time.LocalDate

/**
 * fileName       : NotificationLogRepository
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 배치 발송 이력 포트. exists 는 fcm_result 와 무관하게 키 존재만 본다 (FAILED 도 당일 재발송 없음)
 */
interface NotificationLogRepository {

    fun exists(userId: Long, type: NotificationType, businessDate: LocalDate): Boolean

    fun save(log: NotificationLog): NotificationLog
}
