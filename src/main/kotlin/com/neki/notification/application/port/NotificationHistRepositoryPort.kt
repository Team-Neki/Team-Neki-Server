package com.neki.notification.application.port

import com.neki.notification.domain.entity.NotificationHist

interface NotificationHistRepositoryPort {

    fun save(hist: NotificationHist): NotificationHist

    /** 사용자의 가장 최근 알림을 최신순으로 최대 30건 조회한다. */
    fun findRecentByUserId(userId: Long): List<NotificationHist>
}
