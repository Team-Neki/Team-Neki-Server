package com.neki.notification.repository

import com.neki.notification.models.Notification

interface NotificationRepository {
    fun findByUserId(userId: Long): Notification?

    fun save(notification: Notification): Notification

    fun deleteByUserId(userId: Long)
}
