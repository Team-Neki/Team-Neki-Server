package com.neki.admin.media.api.controller

import com.neki.admin.media.api.dto.AdminMediaConverter
import com.neki.admin.media.api.dto.AdminMediaDto
import com.neki.admin.media.application.AdminMediaUploadFacade
import com.neki.admin.media.application.dto.AdminMediaResult
import com.neki.core.api.dto.BaseResponse
import com.neki.domain.media.dto.MediaCommand
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/media")
class AdminMediaController(private val mediaUploadFacade: AdminMediaUploadFacade) {
    @PostMapping("/upload")
    fun upload(
        @AuthenticationPrincipal(expression = "userId") uploaderId: Long,
        @Valid @RequestBody request: AdminMediaDto.UploadRequest,
    ): BaseResponse<AdminMediaDto.UploadResponse> {
        val command: MediaCommand.GenerateUploadTicket = AdminMediaConverter.toCommand(uploaderId, request)
        val result: AdminMediaResult.UploadTicket = mediaUploadFacade.issueTickets(command)
        return BaseResponse(data = AdminMediaConverter.toResponse(result))
    }
}
