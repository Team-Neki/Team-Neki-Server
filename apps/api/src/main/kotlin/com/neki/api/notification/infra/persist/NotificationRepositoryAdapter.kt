package com.neki.api.notification.infra.persist

import com.neki.api.notification.infra.persist.jpa.JpaNotificationRepository
import com.neki.domain.notification.models.Notification
import com.neki.domain.notification.repository.NotificationRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationRepositoryAdapter(private val jpaRepository: JpaNotificationRepository) : NotificationRepository {

    override fun findByUserId(userId: Long): Notification? = jpaRepository.findByUserId(userId)

    override fun save(notification: Notification): Notification = jpaRepository.save(notification)

    override fun deleteByUserId(userId: Long) = jpaRepository.deleteByUserId(userId)
}
