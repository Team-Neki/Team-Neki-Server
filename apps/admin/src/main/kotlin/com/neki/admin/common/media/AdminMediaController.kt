package com.neki.admin.common.media

import com.neki.core.api.dto.BaseResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/v1/media")
class AdminMediaController(private val uploads: AdminMediaUploadFacade) {
    @PostMapping("/upload")
    fun upload(
        @AuthenticationPrincipal(expression = "userId") uploaderId: Long,
        @Valid @RequestBody request: AdminMediaDto.UploadRequest,
    ): BaseResponse<AdminMediaDto.UploadResponse> = BaseResponse(data = uploads.issueTickets(uploaderId, request))
}
