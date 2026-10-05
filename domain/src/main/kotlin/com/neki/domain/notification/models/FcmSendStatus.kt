package com.neki.domain.notification.models

/**
 * fileName       : FcmSendStatus
 * author         : koo
 * date           : 2026. 10. 6.
 * description    : notification_log.fcm_result. SKIPPED 는 Notification 앱이 남긴 기존 행을 위해 남겨 두며 새로 쓰지 않는다.
 *                  이름이 Result 로 끝나면 ArchUnit 이 application 계층 DTO 로 보므로 Status 로 둔다
 */
enum class FcmSendStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
}
