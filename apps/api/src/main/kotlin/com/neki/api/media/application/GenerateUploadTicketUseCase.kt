package com.neki.api.media.application

import com.neki.api.media.application.dto.MediaAssembler
import com.neki.api.media.application.dto.MediaResult
import com.neki.core.annotation.UseCase
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.models.MediaUploadTickets
import com.neki.domain.media.service.MediaService
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GenerateUploadTicketUseCase
 * author         : koo
 * date           : 2026. 1. 23.
 * description    : 여러 개의 미디어 업로드 ticket 발급 usecase
 */
@UseCase
class GenerateUploadTicketUseCase(private val mediaService: MediaService) {

    // 전체 벌크 작업을 단일 트랜잭션으로 처리
    @Transactional
    fun execute(command: MediaCommand.GenerateUploadTicket): MediaResult.GenerateUploadTicket {
        if (command.items.isEmpty()) throw BusinessException(ResultCode.INVALID_PARAMETER)

        val issuedTickets: MediaUploadTickets = mediaService.issueUploadTickets(command)

        return MediaResult.GenerateUploadTicket(
            method = issuedTickets.firstTicket().method,
            expiresAt = issuedTickets.firstTicket().expiresAt,
            tickets = MediaAssembler.toUploadTicketItems(issuedTickets),
        )
    }
}
