package com.neki.api.notification.infra.persist.jpa

import com.neki.domain.notification.models.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface JpaNotificationRepository : JpaRepository<Notification, Long> {

    fun findByUserId(userId: Long): Notification?

    fun deleteByUserId(userId: Long)
}
