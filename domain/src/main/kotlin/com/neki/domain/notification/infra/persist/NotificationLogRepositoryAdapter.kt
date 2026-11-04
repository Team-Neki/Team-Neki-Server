package com.neki.domain.notification.infra.persist

import com.neki.domain.notification.infra.persist.jpa.JpaNotificationLogRepository
import com.neki.domain.notification.models.NotificationLog
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.repository.NotificationLogRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class NotificationLogRepositoryAdapter(private val jpaRepository: JpaNotificationLogRepository) :
    NotificationLogRepository {

    override fun exists(userId: Long, type: NotificationType, businessDate: LocalDate): Boolean =
        jpaRepository.existsByUserIdAndNotificationTypeAndBusinessDate(userId, type, businessDate)

    override fun save(log: NotificationLog): NotificationLog = jpaRepository.save(log)
}
