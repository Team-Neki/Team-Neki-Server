package com.neki.batch.notification.tasklet

import com.neki.domain.notification.external.PushNotificationSender
import com.neki.domain.notification.models.NotificationType
import com.neki.domain.notification.repository.NotificationLogRepository
import com.neki.domain.notification.service.NotificationService
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * fileName       : PushNotificationTaskletFactory
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : 잡 3개가 공유하는 tasklet 의존(이력, 발송 포트, hist 서비스)을 들고 있다.
 *                  잡은 자기만 다른 것(종류, 날짜, Reader)만 넘긴다. tasklet 은 상태를 가지므로 잡의 @StepScope 빈 안에서 만든다
 */
@Component
class PushNotificationTaskletFactory(
    private val notificationLogRepository: NotificationLogRepository,
    private val pushNotificationSender: PushNotificationSender,
    private val notificationService: NotificationService,
) {

    fun create(type: NotificationType, businessDate: LocalDate, reader: SendTargetReader): PushNotificationTasklet =
        PushNotificationTasklet(
            type,
            businessDate,
            reader,
            PAGE_SIZE,
            notificationLogRepository,
            pushNotificationSender,
            notificationService,
        )

    companion object {
        /** 동의자 DB 조회 단위. 커밋 단위(1건)와 다르다 */
        const val PAGE_SIZE = 100
    }
}
