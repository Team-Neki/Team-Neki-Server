package com.yapp2app.media.api.controller

import com.yapp2app.common.api.document.RequiresSecurity
import com.yapp2app.common.api.dto.BaseResponse
import com.yapp2app.media.api.converter.MediaCommandConverter
import com.yapp2app.media.api.converter.MediaResultConverter
import com.yapp2app.media.api.dto.GenerateUploadTicketRequest
import com.yapp2app.media.api.dto.GenerateUploadTicketResponse
import com.yapp2app.media.application.usecase.GenerateUploadTicketUseCase
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
@RequiresSecurity
@RestController
@RequestMapping("/api/media")
class MediaController(
    private val generateUploadTicketUseCase: GenerateUploadTicketUseCase,
    private val commandConverter: MediaCommandConverter,
    private val resultConverter: MediaResultConverter,
) {

    @PostMapping("/presigned")
    fun generateUploadTicket(
        @AuthenticationPrincipal(expression = "id") ownerId: Long,
        @RequestBody request: GenerateUploadTicketRequest,
    ): BaseResponse<GenerateUploadTicketResponse> {
        val result = generateUploadTicketUseCase.execute(
            commandConverter.toGenerateUploadTicketCommand(ownerId, request),
        )

        val response = resultConverter.toGenerateUploadTicketResponse(result)

        return BaseResponse(data = response)
    }
}
