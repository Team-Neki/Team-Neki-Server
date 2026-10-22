package com.neki.domain.notification.infra.persist.jpa

import com.neki.domain.notification.models.Notification
import org.springframework.data.domain.Limit
import org.springframework.data.jpa.repository.JpaRepository

interface JpaNotificationRepository : JpaRepository<Notification, Long> {

    fun findByUserId(userId: Long): Notification?

    fun deleteByUserId(userId: Long)

    fun findByPushAgreedTrueAndUserIdGreaterThanOrderByUserIdAsc(userId: Long, limit: Limit): List<Notification>
}
