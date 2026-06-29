package com.neki.notification.infra.persist

import com.neki.notification.application.port.NotificationHistRepositoryPort
import com.neki.notification.domain.entity.NotificationHist
import com.neki.notification.infra.persist.jpa.JpaNotificationHistRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationHistRepositoryAdapter(private val jpaRepository: JpaNotificationHistRepository) :
    NotificationHistRepositoryPort {

    override fun save(hist: NotificationHist): NotificationHist = jpaRepository.save(hist)

    override fun findRecentByUserId(userId: Long): List<NotificationHist> =
        jpaRepository.findTop30ByUserIdOrderByCreatedAtDescIdDesc(userId)
}
