package com.neki.api.notification.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

/**
 * fileName       : NotificationRequest
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 알림 관련 요청 DTO
 */
object NotificationRequest {
    @Schema(name = "UpdateNotificationRequest")
    data class UpdateNotification(
        @field:NotBlank(message = "알림 토큰은 필수입니다.")
        @field:Schema(description = "기기 푸시 알림 토큰 (FCM/APNs)", example = "fG1cQ...token")
        val deviceToken: String,

        @field:Schema(description = "푸시 알림 수신 동의 여부 (미입력 시 false)", example = "true", defaultValue = "false")
        val pushAgreed: Boolean = false,
    )
}
