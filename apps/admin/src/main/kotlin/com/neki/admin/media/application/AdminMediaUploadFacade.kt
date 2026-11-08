package com.neki.admin.media.application

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaKey
import com.neki.domain.media.models.MediaType
import com.neki.domain.media.models.MediaUploadTicket
import com.neki.domain.media.models.MediaUploadTickets
import com.neki.domain.media.repository.MediaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminMediaUploadFacade(private val mediaRepository: MediaRepository, private val mediaStorage: MediaStorage) {
    @Transactional
    fun issueTickets(command: MediaCommand.GenerateAdminUploadTicket): MediaUploadTickets {
        val tickets: List<MediaUploadTicket> = command.items.map { item ->
            val type = item.mediaType
            val contentType = item.contentType
            if (type !in ALLOWED_MEDIA_TYPES || contentType !in ALLOWED_CONTENT_TYPES) {
                throw BusinessException(ResultCode.INVALID_PARAMETER)
            }

            val key = MediaKey.generateFromContentType(type, contentType)
            val media = mediaRepository.save(
                Media(
                    storageKey = key,
                    ownerId = command.ownerId,
                    mediaType = type,
                    contentType = contentType,
                    width = item.width,
                    height = item.height,
                    size = item.size,
                    adminManaged = true,
                ),
            )
            MediaUploadTicket(media, mediaStorage.generateUploadTicket(key, contentType))
        }

        return MediaUploadTickets(tickets)
    }

    companion object {
        private val ALLOWED_MEDIA_TYPES: Set<MediaType> = setOf(MediaType.LOGO, MediaType.POSE)
        private val ALLOWED_CONTENT_TYPES: Set<String> = setOf("image/jpeg", "image/png", "image/webp")
    }
}
