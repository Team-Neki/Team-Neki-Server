package com.neki.notification.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.notification.api.converter.NotificationCommandConverter
import com.neki.notification.api.dto.UpdateNotificationRequest
import com.neki.notification.application.command.UpdateNotificationCommand
import com.neki.notification.application.usecase.UpdateNotificationUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : NotificationController
 * author         : darren
 * date           : 2026. 6. 12
 * description    : 알림 API endpoint
 */
@RequiresSecurity
@Tag(name = "notification", description = "알림 API")
@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val updateNotificationUseCase: UpdateNotificationUseCase,
    private val commandConverter: NotificationCommandConverter,
) {

    @Operation(
        summary = "알림 등록/수정 API",
        description = "사용자의 기기 푸시 알림 토큰과 푸시 알림 수신 동의 여부를 저장합니다. 이미 등록된 사용자는 갱신됩니다.",
    )
    @PatchMapping
    fun updateNotification(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: UpdateNotificationRequest,
    ): BaseResponse<Any> {
        val command: UpdateNotificationCommand =
            commandConverter.toUpdateNotificationCommand(userId, request)

        updateNotificationUseCase.execute(command)

        return BaseResponse()
    }
}
