package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.media.application.command.BulkGenerateUploadTicketCommand
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.application.result.BulkGenerateUploadTicketResult
import com.yapp2app.media.domain.MediaKey
import com.yapp2app.media.domain.entity.Media

/**
 * fileName       : BulkGenerateUploadTicketUseCase
 * author         : koo
 * date           : 2026. 1. 23.
 * description    : 여러 개의 미디어 업로드 ticket 발급 usecase
 */
@UseCase
class BulkGenerateUploadTicketUseCase(
    private val mediaStorage: MediaStoragePort,
    private val mediaRepository: MediaRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: BulkGenerateUploadTicketCommand): BulkGenerateUploadTicketResult {
        // 전체 벌크 작업을 단일 트랜잭션으로 처리
        return transactionRunner.run {
            val tickets = command.items.map { item ->
                // 1. storageKey 생성
                val storageKey = MediaKey.generate(item.mediaType, item.filename, item.contentType)

                // 2. Media 엔티티 생성 및 저장 (상태: INITIATED)
                val media = Media(
                    storageKey = storageKey,
                    ownerId = command.ownerId,
                    mediaType = item.mediaType,
                    contentType = item.contentType,
                )
                val savedMedia = mediaRepository.save(media)

                // 3. Upload Ticket 발급
                val uploadTicket = mediaStorage.generateUploadTicket(
                    key = storageKey,
                    contentType = item.contentType,
                )

                // 4. 결과 항목 생성
                BulkGenerateUploadTicketResult.UploadTicketInfo(
                    mediaId = savedMedia.id!!,
                    uploadUrl = uploadTicket.url,
                    method = uploadTicket.method,
                    expiresAt = uploadTicket.expiresAt,
                    contentType = item.contentType,
                )
            }

            BulkGenerateUploadTicketResult(tickets = tickets)
        }
    }
}
