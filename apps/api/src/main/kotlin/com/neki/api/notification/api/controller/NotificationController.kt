package com.neki.api.notification.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.notification.api.dto.NotificationConverter
import com.neki.api.notification.api.dto.NotificationRequest
import com.neki.api.notification.api.dto.NotificationResponse
import com.neki.api.notification.application.GetRecentNotificationsUseCase
import com.neki.api.notification.application.SendPushUseCase
import com.neki.api.notification.application.UpdateNotificationUseCase
import com.neki.api.notification.application.dto.NotificationResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.notification.dto.NotificationCommand
import com.neki.domain.notification.dto.NotificationQuery
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
    private val sendPushUseCase: SendPushUseCase,
    private val getRecentNotificationsUseCase: GetRecentNotificationsUseCase,
    private val requestConverter: NotificationConverter.RequestConverter,
    private val responseConverter: NotificationConverter.ResponseConverter,
) {

    @Operation(
        summary = "알림 등록/수정 API",
        description = "사용자의 기기 푸시 알림 토큰과 푸시 알림 수신 동의 여부를 저장합니다. 이미 등록된 사용자는 갱신됩니다.",
    )
    @PatchMapping
    fun updateNotification(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: NotificationRequest.UpdateNotification,
    ): BaseResponse<Any> {
        val command: NotificationCommand.UpdateNotification =
            requestConverter.toUpdateNotificationCommand(userId, request)

        updateNotificationUseCase.execute(command)

        return BaseResponse()
    }

    @Operation(
        summary = "FCM 푸시 발송 API",
        description = "전달받은 디바이스 토큰으로 푸시 알림을 발송하고, 수신자(userId)의 알림 내역으로 저장합니다. " +
            "저장된 내역은 '최근 알림 조회 API'로 확인할 수 있습니다. " +
            "link 를 지정하면 알림 탭 시 앱이 해당 딥링크로 이동합니다. (예: neki://archive/123)",
    )
    @PostMapping("/push")
    fun sendPush(
        @RequestParam userId: Long,
        @RequestParam token: String,
        @RequestParam type: String,
        @RequestParam(required = false, defaultValue = "알림") title: String,
        @RequestParam(required = false, defaultValue = "FCM 푸시 발송입니다.") body: String,
        @RequestParam(required = false) link: String?,
    ): BaseResponse<NotificationResult.SendPush> {
        val result: NotificationResult.SendPush =
            sendPushUseCase.execute(NotificationCommand.SendPush(userId, token, type, title, body, link))

        return BaseResponse(data = result)
    }

    @Operation(
        summary = "최근 알림 조회 API",
        description = "로그인 사용자가 발송받은 알림 내역을 최신순으로 최대 30건 조회합니다.",
    )
    @GetMapping("/recent")
    fun getRecentNotifications(
        @AuthenticationPrincipal(expression = "id") userId: Long,
    ): BaseResponse<List<NotificationResponse.GetRecentNotification>> {
        val query = NotificationQuery.GetRecentNotifications(userId)

        val result: List<NotificationResult.GetRecentNotification> = getRecentNotificationsUseCase.execute(query)

        val response: List<NotificationResponse.GetRecentNotification> =
            responseConverter.toGetRecentNotificationResponse(result)

        return BaseResponse(data = response)
    }
}
