package com.neki.domain.notification.repository

import com.neki.domain.notification.models.Notification

interface NotificationRepository {
    fun findByUserId(userId: Long): Notification?

    fun save(notification: Notification): Notification

    fun deleteByUserId(userId: Long)
}
