package com.neki.domain.media.service

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaKey
import com.neki.domain.media.models.MediaStorageUploadTicket
import com.neki.domain.media.models.MediaUploadTicket
import com.neki.domain.media.models.MediaUploadTickets
import com.neki.domain.media.models.UploadConfirmStatus
import com.neki.domain.media.repository.MediaRepository
import org.springframework.stereotype.Component

/**
 * fileName       : MediaService
 * author         : koo
 * date           : 2026. 8. 3. 오전 12:56
 * description    : Media 도메인 서비스
 */
@Component
class MediaService(private val mediaRepository: MediaRepository, private val mediaStorage: MediaStorage) {

    fun getActiveMedia(query: MediaQuery.GetMediaMetadata): Media {
        val media: Media? = query.ownerId?.let {
            mediaRepository.getActiveMedia(it, query.mediaId)
        } ?: mediaRepository.getActiveMedia(query.mediaId)

        return media ?: throw BusinessException(ResultCode.NOT_FOUND)
    }

    fun getActiveMedias(query: MediaQuery.GetMediaMetadataList): List<Media> = query.ownerId?.let {
        mediaRepository.getActiveMedias(it, query.mediaIds)
    } ?: mediaRepository.getActiveMedias(query.mediaIds)

    fun getActiveMedias(query: MediaQuery.GetMedias): List<Media> =
        mediaRepository.getActiveMedias(query.ownerId, query.mediaIds)

    fun issueUploadTickets(command: MediaCommand.GenerateUploadTicket): MediaUploadTickets {
        val tickets: List<MediaUploadTicket> = command.items.map { item ->
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

            val uploadTicket: MediaStorageUploadTicket = mediaStorage.generateUploadTicket(
                key = storageKey,
                contentType = item.contentType,
            )

            MediaUploadTicket(savedMedia, uploadTicket)
        }

        return MediaUploadTickets(tickets)
    }

    fun getExistsMap(command: MediaCommand.ConfirmMediasUploaded): Map<Long, Boolean> {
        val medias: List<Media> = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaIds)

        return medias
            .filter { !it.isUploaded() }
            .associate { it.id!! to mediaStorage.exists(it.storageKey) }
    }

    fun confirmMediasUploaded(
        command: MediaCommand.ConfirmMediasUploaded,
        storageExistsMap: Map<Long, Boolean>,
    ): Map<Long, UploadConfirmStatus> {
        val freshMedias: List<Media> = mediaRepository.getMediaForUploadConfirmation(
            command.ownerId,
            command.mediaIds,
        )

        val freshMediaMap: Map<Long, Media> = freshMedias.associateBy { it.id!! }

        return command.mediaIds.associateWith { mediaId ->
            val media: Media? = freshMediaMap[mediaId]
            if (media == null) {
                UploadConfirmStatus.NOT_FOUND
            } else {
                confirmUpload(media, storageExistsMap[mediaId] == true)
            }
        }
    }

    private fun confirmUpload(media: Media, storageExists: Boolean): UploadConfirmStatus {
        val wasUploaded: Boolean = media.isUploaded()

        val status: UploadConfirmStatus = media.confirmUpload(storageExists)

        if (!wasUploaded && status == UploadConfirmStatus.CONFIRMED) {
            mediaRepository.save(media)
        }

        return status
    }

    fun rollbackToInitiated(command: MediaCommand.ConfirmMediasUploaded) {
        val medias: List<Media> = mediaRepository.getMediaForUploadConfirmation(command.ownerId, command.mediaIds)
        medias.forEach {
            it.markAsInitiated()
            mediaRepository.save(it)
        }
    }

    fun deleteMedias(command: MediaCommand.DeleteMedias): List<Media> {
        val foundMedias: List<Media> = mediaRepository.getActiveMedias(command.ownerId, command.mediaIds)
        foundMedias.forEach { it.markAsDeleted() }
        return mediaRepository.saveAll(foundMedias)
    }
}
