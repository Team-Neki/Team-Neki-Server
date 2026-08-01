package com.neki.notification.infra.persist.jpa

import com.neki.notification.entity.NotificationHist
import org.springframework.data.jpa.repository.JpaRepository

/**
 * fileName       : JpaNotificationHistRepository
 * author         : darren
 * date           : 2026. 6. 22
 * description    : 발송 알림 내역 JPA Repository
 */
interface JpaNotificationHistRepository : JpaRepository<NotificationHist, Long> {

    fun findTop30ByUserIdOrderByCreatedAtDescIdDesc(userId: Long): List<NotificationHist>
}
