package com.neki.admin.common.media

import com.neki.core.api.dto.BaseResponse
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.models.MediaUploadTickets
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
    ): BaseResponse<AdminMediaDto.UploadResponse> {
        val command = MediaCommand.GenerateAdminUploadTicket(
            ownerId = uploaderId,
            items = request.items.map { item ->
                MediaCommand.GenerateAdminUploadTicket.Item(
                    contentType = item.contentType!!,
                    mediaType = item.mediaType!!,
                    width = item.width,
                    height = item.height,
                    size = item.size,
                )
            },
        )
        val tickets: MediaUploadTickets = uploads.issueTickets(command)
        val response = AdminMediaDto.UploadResponse(
            method = tickets.firstTicket().method,
            expiresAt = tickets.firstTicket().expiresAt,
            items = tickets.tickets.map { (media, ticket) ->
                AdminMediaDto.UploadResponse.Item(media.id!!, ticket.url, ticket.contentType)
            },
        )
        return BaseResponse(data = response)
    }
}
