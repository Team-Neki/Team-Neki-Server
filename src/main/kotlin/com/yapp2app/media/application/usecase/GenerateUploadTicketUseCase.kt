package com.yapp2app.media.application.usecase

import com.yapp2app.common.annotation.UseCase
import com.yapp2app.common.transaction.TransactionRunner
import com.yapp2app.media.application.command.GenerateUploadTicketCommand
import com.yapp2app.media.application.port.MediaRepositoryPort
import com.yapp2app.media.application.port.MediaStoragePort
import com.yapp2app.media.application.result.GenerateUploadTicketResult
import com.yapp2app.media.domain.MediaKey
import com.yapp2app.media.domain.entity.Media

/**
 * fileName       : GenerateUploadTicketUseCase
 * author         : koo
 * date           : 2026. 1. 2. 오후 7:43
 * description    : object storage 저장 usecase
 */
@UseCase
class GenerateUploadTicketUseCase(
    private val mediaStorage: MediaStoragePort,
    private val mediaRepository: MediaRepositoryPort,
    private val transactionRunner: TransactionRunner,
) {

    fun execute(command: GenerateUploadTicketCommand): GenerateUploadTicketResult {
        // storageKey 생성
        val storageKey = MediaKey.generate(command.mediaType, command.filename)

        // Media 엔티티 생성 및 저장
        val media = Media(
            storageKey = storageKey,
            ownerId = command.ownerId,
            mediaType = command.mediaType,
            contentType = command.contentType,
        )

        // TODO : ttl 적용할 수 있도록 변경
        val savedMedia = transactionRunner.run { mediaRepository.save(media) }

        // Upload Ticket 발급
        val uploadTicket = mediaStorage.generateUploadTicket(
            key = storageKey,
            contentType = command.contentType,
        )

        return GenerateUploadTicketResult(
            mediaId = savedMedia.id!!,
            uploadUrl = uploadTicket.url,
            method = uploadTicket.method,
            expiresAt = uploadTicket.expiresAt,
            contentType = command.contentType,
        )
    }
}
