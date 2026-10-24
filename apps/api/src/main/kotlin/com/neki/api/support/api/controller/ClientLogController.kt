package com.neki.api.support.api.controller

import com.neki.api.support.api.dto.ClientLogConverter
import com.neki.api.support.api.dto.ClientLogRequest
import com.neki.api.support.application.CollectClientLogsUseCase
import com.neki.core.api.dto.BaseResponse
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.core.security.AuthenticatedUser
import com.neki.domain.support.dto.ClientLogCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : ClientLogController
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트(iOS/Android) 로그 수집 API endpoint
 */
@Tag(name = "client-log", description = "클라이언트 행동 이벤트 수집 API")
@RestController
@RequestMapping("/api/logs")
class ClientLogController(
    private val collectClientLogsUseCase: CollectClientLogsUseCase,
    private val requestConverter: ClientLogConverter.RequestConverter,
) {

    @Operation(
        summary = "클라이언트 행동 이벤트 수집 API",
        description = """
            앱에서 발생한 행동 이벤트를 모아 Firehose 로 보냅니다. Authorization 은 선택이지만,
            보내면 유효한 Bearer 토큰이어야 합니다. 서버의 userId 는 전송 시점에 인증된 ID 이며,
            log.userIdAtOccurrence 는 발생 당시 앱이 기록한 별도 값입니다.

            - eventId, occurredAt, sessionId, sequenceNumber, deviceId, appVersion 은 이벤트 발생 시점에 기록하고 재전송에도 유지하세요
            - sessionId 는 세션 시작 시각의 epoch milliseconds 입니다. 서버는 수신 시각으로 세션을 만들지 않습니다
            - eventProperties 와 context 는 JSON 타입을 유지하며 그대로 저장합니다
            - 요청당 1~500건, 직렬화 후 로그 1건 1,000KiB · 요청 전체 4MiB 이하
            - 503 D-15 (LOG_SEND_FAILED): 일시적 실패. 같은 배치를 나중에 다시 보내세요 (일부 중복 적재될 수 있음)
            - 413 D-16 (LOG_PAYLOAD_TOO_LARGE): 배치를 나눠 다시 보내세요. 단일 이벤트도 넘으면 앱에서 진단하세요
            - 400 D-01 (INVALID_PARAMETER): 필수 필드·형식 오류입니다
        """,
    )
    @PostMapping
    fun collectLogs(
        @AuthenticationPrincipal principal: AuthenticatedUser?,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
        @Valid @RequestBody request: ClientLogRequest.Collect,
    ): BaseResponse<Any> {
        if (authorization != null &&
            (!authorization.startsWith("Bearer ") || authorization.removePrefix("Bearer ").isBlank())
        ) {
            throw BusinessException(ResultCode.INVALID_TOKEN_ERROR)
        }
        val command: ClientLogCommand.Collect = requestConverter.toCollectCommand(principal?.id, request)

        collectClientLogsUseCase.execute(command)

        return BaseResponse()
    }
}
