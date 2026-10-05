package com.neki.api.media.application

import com.neki.api.media.application.dto.MediaResult
import com.neki.core.annotation.UseCase
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.models.MediaStorageUploadTicket
import com.neki.domain.media.service.MediaService

/**
 * fileName       : GenerateQrDumpUploadTicketUseCase
 * author         : koo
 * date           : 2026. 10. 2.
 * description    : QR 파싱 실패 HTML 덤프 업로드 ticket 발급 usecase
 */
@UseCase
class GenerateQrDumpUploadTicketUseCase(private val mediaService: MediaService) {

    fun execute(command: MediaCommand.GenerateQrDumpUploadTicket): MediaResult.GenerateQrDumpUploadTicket {
        val ticket: MediaStorageUploadTicket = mediaService.issueQrDumpUploadTicket(command)

        return MediaResult.GenerateQrDumpUploadTicket(
            method = ticket.method,
            expiresAt = ticket.expiresAt,
            uploadUrl = ticket.url,
            contentType = ticket.contentType,
        )
    }
}
