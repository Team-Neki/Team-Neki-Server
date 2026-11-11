package com.neki.admin.media.application

import com.neki.admin.media.application.dto.AdminMediaResult
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.models.MediaType
import com.neki.domain.media.models.MediaUploadTickets
import com.neki.domain.media.service.MediaService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminMediaUploadFacade(private val mediaService: MediaService) {
    @Transactional
    fun issueTickets(command: MediaCommand.GenerateUploadTicket): AdminMediaResult.UploadTicket {
        if (command.items.any { it.mediaType !in ALLOWED_MEDIA_TYPES || it.contentType !in ALLOWED_CONTENT_TYPES }) {
            throw BusinessException(ResultCode.INVALID_PARAMETER)
        }
        val tickets: MediaUploadTickets = mediaService.issueUploadTickets(command)
        return AdminMediaResult.UploadTicket(
            method = tickets.firstTicket().method,
            expiresAt = tickets.firstTicket().expiresAt,
            items = tickets.tickets.map { (media, ticket) ->
                AdminMediaResult.UploadTicket.Item(media.id!!, ticket.url, ticket.contentType)
            },
        )
    }

    companion object {
        private val ALLOWED_MEDIA_TYPES: Set<MediaType> = setOf(MediaType.LOGO, MediaType.POSE)
        private val ALLOWED_CONTENT_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp")
    }
}
