package com.neki.notification.infra.persist

import com.neki.notification.application.port.NotificationRepositoryPort
import com.neki.notification.domain.entity.Notification
import com.neki.notification.infra.persist.jpa.JpaNotificationRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationRepositoryAdapter(private val jpaRepository: JpaNotificationRepository) :
    NotificationRepositoryPort {

    override fun findByUserId(userId: Long): Notification? = jpaRepository.findByUserId(userId)

    override fun save(notification: Notification): Notification = jpaRepository.save(notification)
}
