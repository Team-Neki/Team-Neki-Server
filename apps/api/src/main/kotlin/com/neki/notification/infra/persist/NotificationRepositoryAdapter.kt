package com.neki.notification.infra.persist

import com.neki.notification.NotificationRepository
import com.neki.notification.infra.persist.jpa.JpaNotificationRepository
import com.neki.notification.models.Notification
import org.springframework.stereotype.Repository

@Repository
class NotificationRepositoryAdapter(private val jpaRepository: JpaNotificationRepository) : NotificationRepository {

    override fun findByUserId(userId: Long): Notification? = jpaRepository.findByUserId(userId)

    override fun save(notification: Notification): Notification = jpaRepository.save(notification)

    override fun deleteByUserId(userId: Long) = jpaRepository.deleteByUserId(userId)
}
