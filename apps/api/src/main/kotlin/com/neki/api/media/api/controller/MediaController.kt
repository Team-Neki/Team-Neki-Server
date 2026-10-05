package com.neki.api.media.api.controller

import com.neki.api.common.api.document.RequiresSecurity
import com.neki.api.media.api.dto.MediaConverter
import com.neki.api.media.api.dto.MediaRequest
import com.neki.api.media.api.dto.MediaResponse
import com.neki.api.media.application.GenerateQrDumpUploadTicketUseCase
import com.neki.api.media.application.GenerateUploadTicketUseCase
import com.neki.api.media.application.dto.MediaResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.media.dto.MediaCommand
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
    private val generateQrDumpUploadTicketUseCase: GenerateQrDumpUploadTicketUseCase,
    private val requestConverter: MediaConverter.RequestConverter,
    private val responseConverter: MediaConverter.ResponseConverter,
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
        @Valid @RequestBody request: MediaRequest.UploadTicket,
    ): BaseResponse<MediaResponse.UploadTicket> {
        val command: MediaCommand.GenerateUploadTicket = requestConverter.toGenerateUploadTicketCommand(
            ownerId,
            request,
        )

        val result: MediaResult.GenerateUploadTicket = generateUploadTicketUseCase.execute(command)

        val response: MediaResponse.UploadTicket = responseConverter.toUploadTicketResponse(result)

        return BaseResponse(data = response)
    }

    @Operation(
        summary = "QR 파싱 실패 HTML 덤프 업로드 ticket 발급",
        description = """
            QR 페이지 파싱에 실패했을 때 원본 HTML을 S3에 올리기 위한 ticket을 발급받습니다.
            Request body는 없습니다.

            Workflow:
            1. 이 API를 호출하여 업로드 ticket 발급
            2. uploadTicket URL로 raw HTML을 body에 담아 PUT 요청 (S3 직접 업로드). 별도 등록 API는 없습니다.

            주의:
            * PUT 요청의 Content-Type 헤더는 응답의 contentType 값과 정확히 같아야 합니다. 다르면 S3가 403을 반환합니다.
            * 원본 QR URL은 HTML 맨 앞에 <!-- source: {qrUrl} --> 형태로 붙여 올려주세요.
        """,
    )
    @PostMapping("/qr-dumps/upload")
    fun generateQrDumpUploadTicket(
        @AuthenticationPrincipal(expression = "id") ownerId: Long,
    ): BaseResponse<MediaResponse.QrDumpUploadTicket> {
        val command: MediaCommand.GenerateQrDumpUploadTicket =
            requestConverter.toGenerateQrDumpUploadTicketCommand(ownerId)

        val result: MediaResult.GenerateQrDumpUploadTicket = generateQrDumpUploadTicketUseCase.execute(command)

        val response: MediaResponse.QrDumpUploadTicket = responseConverter.toQrDumpUploadTicketResponse(result)

        return BaseResponse(data = response)
    }
}
