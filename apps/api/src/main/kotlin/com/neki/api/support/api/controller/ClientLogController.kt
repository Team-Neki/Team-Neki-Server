package com.neki.api.support.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.support.api.dto.ClientLogConverter
import com.neki.api.support.api.dto.ClientLogRequest
import com.neki.api.support.application.CollectClientLogsUseCase
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.support.dto.ClientLogCommand
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : ClientLogController
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : 클라이언트(iOS/Android) 로그 수집 API endpoint
 */
@RequiresSecurity
@Tag(name = "client-log", description = "클라이언트 로그 수집 API")
@RestController
@RequestMapping("/api/logs")
class ClientLogController(
    private val collectClientLogsUseCase: CollectClientLogsUseCase,
    private val requestConverter: ClientLogConverter.RequestConverter,
) {

    @Operation(
        summary = "클라이언트 로그 수집 API",
        description = """
            앱 로그를 모아 한 번에 보냅니다. 서버는 로그마다 userId, platform, receivedAt 을 붙여
            Kinesis Firehose 로 전달합니다. logs 의 각 원소는 형식 제한이 없는 JSON 객체입니다.

            - 각 로그에 appVersion(로그가 발생한 시점의 앱 버전)을 넣어 주세요. 쌓아 둔 로그를 업데이트 뒤에 보내도 버전이 틀어지지 않습니다
            - 요청당 1~500건, 직렬화 후 로그 1건 1,000KiB · 요청 전체 4MiB 이하
            - D-14 (LOG_SEND_FAILED): 일시적 실패. 같은 배치를 나중에 다시 보내세요 (일부 중복 적재될 수 있음)
            - D-01 (INVALID_PARAMETER): 형식·크기 오류. 다시 보내도 실패하므로 버리세요
        """,
    )
    @PostMapping
    fun collectLogs(
        @AuthenticationPrincipal(expression = "id") userId: Long,
        @Valid @RequestBody request: ClientLogRequest.Collect,
    ): BaseResponse<Any> {
        val command: ClientLogCommand.Collect = requestConverter.toCollectCommand(userId, request)

        collectClientLogsUseCase.execute(command)

        return BaseResponse()
    }
}
