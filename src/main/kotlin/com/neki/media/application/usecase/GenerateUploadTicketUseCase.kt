package com.neki.media.application.usecase

import com.neki.common.annotation.UseCase
import com.neki.common.api.dto.ResultCode
import com.neki.common.exception.BusinessException
import com.neki.common.transaction.TransactionRunner
import com.neki.media.application.command.GenerateUploadTicketCommand
import com.neki.media.application.contract.UploadTicket
import com.neki.media.application.port.MediaRepositoryPort
import com.neki.media.application.port.MediaStoragePort
import com.neki.media.application.result.GenerateUploadTicketResult
import com.neki.media.domain.MediaKey
import com.neki.media.domain.entity.Media
import org.springframework.transaction.annotation.Transactional

/**
 * fileName       : GenerateUploadTicketUseCase
 * author         : koo
 * date           : 2026. 1. 23.
 * description    : 여러 개의 미디어 업로드 ticket 발급 usecase
 */
@UseCase
class GenerateUploadTicketUseCase(
    private val mediaStorage: MediaStoragePort,
    private val mediaRepository: MediaRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {

    @Transactional
    fun execute(command: GenerateUploadTicketCommand): GenerateUploadTicketResult {
        if (command.items.isEmpty()) throw BusinessException(ResultCode.INVALID_PARAMETER)

        // 전체 벌크 작업을 단일 트랜잭션으로 처리
        var method: String? = null
        var expiresAt: java.time.Instant? = null

        val tickets = command.items.map { item ->
            // storageKey 생성
            val storageKey: String = MediaKey.generate(item.mediaType, item.filename, item.contentType)

            val media = Media(
                storageKey = storageKey,
                ownerId = command.ownerId,
                mediaType = item.mediaType,
                contentType = item.contentType,
                width = item.width,
                height = item.height,
                size = item.size,
            )
            val savedMedia: Media = mediaRepository.save(media)

            // Upload Ticket 발급
            val uploadTicket: UploadTicket = mediaStorage.generateUploadTicket(
                key = storageKey,
                contentType = item.contentType,
            )

            // 첫 번째 티켓에서 method와 expiresAt 추출
            if (method == null) {
                method = uploadTicket.method
                expiresAt = uploadTicket.expiresAt
            }

            GenerateUploadTicketResult.UploadTicketInfo(
                mediaId = savedMedia.id!!,
                uploadUrl = uploadTicket.url,
                contentType = item.contentType,
            )
        }

        return GenerateUploadTicketResult(
            method = method!!,
            expiresAt = expiresAt!!,
            tickets = tickets,
        )
    }
}
