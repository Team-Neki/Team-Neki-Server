package com.neki.domain.notification.repository

import com.neki.domain.notification.models.Notification

interface NotificationRepository {
    fun findByUserId(userId: Long): Notification?

    fun save(notification: Notification): Notification

    fun deleteByUserId(userId: Long)

    /** 배치 발송 대상의 단일 출처. push_agreed = true 이고 user_id > afterUserId 인 행을 user_id 오름차순으로 limit 건 */
    fun findPushAgreedAfter(afterUserId: Long, limit: Int): List<Notification>
}
