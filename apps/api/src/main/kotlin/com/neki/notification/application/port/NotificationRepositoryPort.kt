package com.neki.notification.application.port

import com.neki.notification.entity.Notification

interface NotificationRepositoryPort {
    fun findByUserId(userId: Long): Notification?

    fun save(notification: Notification): Notification

    fun deleteByUserId(userId: Long)
}
