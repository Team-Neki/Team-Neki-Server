package com.neki.notification.application

import com.neki.common.annotation.UseCase
import com.neki.common.transaction.TransactionRunner
import com.neki.notification.dto.NotificationCommand
import com.neki.notification.service.NotificationService
import org.springframework.dao.DataIntegrityViolationException

/**
 * fileName       : UpdateNotificationUseCase
 * author         : darren
 * date           : 2026. 6. 12
 * description    : 알림 토큰 및 푸시 동의 여부 등록/수정 (upsert)
 */
@UseCase
class UpdateNotificationUseCase(
    private val notificationService: NotificationService,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: NotificationCommand.UpdateNotification) {
        try {
            transactionRunner.runNew<Unit> { notificationService.saveOrUpdate(command) }
        } catch (_: DataIntegrityViolationException) {
            // 동시 요청으로 다른 트랜잭션이 먼저 insert 한 경우(user_id UNIQUE 충돌).
            // 별도 트랜잭션에서 이미 커밋된 행을 다시 조회해 update 로 재시도한다.
            transactionRunner.runNew<Unit> { notificationService.saveOrUpdate(command) }
        }
    }
}
