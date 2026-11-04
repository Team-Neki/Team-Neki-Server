package com.neki.domain.notification.infra.persist.jpa

import com.neki.domain.notification.models.NotificationLog
import com.neki.domain.notification.models.NotificationType
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface JpaNotificationLogRepository : JpaRepository<NotificationLog, Long> {

    fun existsByUserIdAndNotificationTypeAndBusinessDate(
        userId: Long,
        notificationType: NotificationType,
        businessDate: LocalDate,
    ): Boolean
}
