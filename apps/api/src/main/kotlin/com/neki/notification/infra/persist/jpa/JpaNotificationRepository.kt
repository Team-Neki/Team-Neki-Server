package com.neki.notification.infra.persist.jpa

import com.neki.notification.models.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface JpaNotificationRepository : JpaRepository<Notification, Long> {

    fun findByUserId(userId: Long): Notification?

    fun deleteByUserId(userId: Long)
}
