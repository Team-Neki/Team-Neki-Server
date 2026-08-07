package com.neki.notification.infra.persist

import com.neki.notification.infra.persist.jpa.JpaNotificationHistRepository
import com.neki.notification.models.NotificationHist
import com.neki.notification.repository.NotificationHistRepository
import org.springframework.stereotype.Repository

@Repository
class NotificationHistRepositoryAdapter(private val jpaRepository: JpaNotificationHistRepository) :
    NotificationHistRepository {

    override fun save(hist: NotificationHist): NotificationHist = jpaRepository.save(hist)

    override fun findRecentByUserId(userId: Long): List<NotificationHist> =
        jpaRepository.findTop30ByUserIdOrderByCreatedAtDescIdDesc(userId)
}
