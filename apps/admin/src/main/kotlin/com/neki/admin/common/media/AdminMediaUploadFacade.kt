package com.neki.admin.common.media

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.dto.MediaCommand
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaKey
import com.neki.domain.media.models.MediaType
import com.neki.domain.media.models.MediaUploadTicket
import com.neki.domain.media.models.MediaUploadTickets
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminMediaUploadFacade(
    private val mediaRepository: JpaAdminMediaRepository,
    private val mediaStorage: MediaStorage,
) {
    @Transactional
    fun issueTickets(command: MediaCommand.GenerateAdminUploadTicket): MediaUploadTickets {
        if (command.items.isEmpty()) throw BusinessException(ResultCode.INVALID_PARAMETER)

        val tickets: List<MediaUploadTicket> = command.items.map { item ->
            val type = item.mediaType
            val contentType = item.contentType
            if (type !in setOf(MediaType.LOGO, MediaType.POSE) ||
                contentType !in setOf("image/jpeg", "image/png", "image/webp")
            ) {
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
}
