package com.neki.notification.api.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * fileName       : NotificationResponse
 * author         : koo
 * date           : 2026. 7. 21.
 * description    : 알림 관련 응답 DTO
 */
object NotificationResponse {
    @Schema(name = "GetRecentNotificationResponse")
    data class GetRecentNotification(
        @field:Schema(description = "알림 내역 ID", example = "1")
        val id: Long,

        @field:Schema(description = "알림 종류 코드", example = "ARCHIVE")
        val type: String,

        @field:Schema(description = "알림 제목", example = "사진이 정리되었어요")
        val title: String,

        @field:Schema(description = "알림 내용", example = "강남에서 찍은 네컷 사진이 보관함에 저장되었어요.")
        val body: String,

        @field:Schema(description = "알림 탭 시 이동할 딥링크", example = "neki://archive/123")
        val link: String?,

        @field:Schema(description = "발송 일시", example = "2026-06-22T16:41:57")
        val createdAt: LocalDateTime,
    )
}
