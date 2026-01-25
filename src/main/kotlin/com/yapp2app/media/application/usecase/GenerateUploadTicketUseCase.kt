package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.media.application.command.GenerateUploadTicketCommand
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.application.result.GenerateUploadTicketResult
import com.yapp2app.media.domain.MediaKey
import com.yapp2app.media.domain.entity.Media
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
        // 전체 벌크 작업을 단일 트랜잭션으로 처리
        var method: String? = null
        var expiresAt: java.time.Instant? = null

        val tickets = command.items.map { item ->
            // storageKey 생성
            val storageKey = MediaKey.generate(item.mediaType, item.filename, item.contentType)

            val media = Media(
                storageKey = storageKey,
                ownerId = command.ownerId,
                mediaType = item.mediaType,
                contentType = item.contentType,
            )
            val savedMedia = mediaRepository.save(media)

            // Upload Ticket 발급
            val uploadTicket = mediaStorage.generateUploadTicket(
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
