package com.neki.media.api.controller

import com.neki.common.api.document.RequiresSecurity
import com.neki.common.api.dto.BaseResponse
import com.neki.media.api.converter.MediaCommandConverter
import com.neki.media.api.converter.MediaResultConverter
import com.neki.media.api.dto.UploadTicketRequest
import com.neki.media.api.dto.UploadTicketResponse
import com.neki.media.application.command.GenerateUploadTicketCommand
import com.neki.media.application.result.GenerateUploadTicketResult
import com.neki.media.application.usecase.GenerateUploadTicketUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * fileName       : MediaController
 * author         : koo
 * date           : 2026. 1. 2. 오후 7:34
 * description    : Media api endpoint
 */
@Tag(name = "MediaController", description = "미디어 업로드 API")
@RequiresSecurity
@RestController
@RequestMapping("/api/media")
class MediaController(
    private val generateUploadTicketUseCase: GenerateUploadTicketUseCase,
    private val commandConverter: MediaCommandConverter,
    private val resultConverter: MediaResultConverter,
) {

    @Operation(
        summary = "미디어 업로드 ticket 발급",
        description = """
            미디어 업로드를 위한 ticket을 발급받습니다.
            Binary를 body에 담아 발급받은 uploadTicket URL로 PUT 요청을 하면 됩니다.

            Workflow:
            1. 이 API를 호출하여 업로드 ticket 발급
            2. 각 ticket의 uploadTicket URL로 파일 업로드 (S3 직접 업로드)
            3. POST /api/photos API 호출하여 메타데이터 등록

            mediaType:
            * USER_PROFILE("user-profiles") : 사용자 프로필
            * PHOTO_BOOTH("photo-booth") : 인생네컷
            * ATTACHMENT("attachments") : 확장성을 고려한 첨부 이미지
            * LOGO("logo") : 로고
            * POSE("pose") : 포즈
            * TEMP("temp") : 업로드 검증, 테스트 등

            contentType:
            * image/jpeg
            * image/png
        """,
    )
    @PostMapping("/upload")
    fun generateUploadTicket(
        @AuthenticationPrincipal(expression = "id") ownerId: Long,
        @Valid @RequestBody request: UploadTicketRequest,
    ): BaseResponse<UploadTicketResponse> {
        val command: GenerateUploadTicketCommand = commandConverter.toGenerateUploadTicketCommand(ownerId, request)

        val result: GenerateUploadTicketResult = generateUploadTicketUseCase.execute(command)

        val response: UploadTicketResponse = resultConverter.toUploadTicketResponse(result)

        return BaseResponse(data = response)
    }
}
