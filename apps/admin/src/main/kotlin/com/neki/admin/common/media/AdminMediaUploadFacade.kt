package com.neki.admin.common.media

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaKey
import com.neki.domain.media.models.MediaStorageUploadTicket
import com.neki.domain.media.models.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminMediaUploadFacade(
    private val mediaRepository: JpaAdminMediaRepository,
    private val mediaStorage: MediaStorage,
) {
    @Transactional
    fun issueTickets(uploaderId: Long, request: AdminMediaDto.UploadRequest): AdminMediaDto.UploadResponse {
        val tickets: List<Pair<Media, MediaStorageUploadTicket>> = request.items.map { item ->
            val type = item.mediaType!!
            val contentType = item.contentType!!
            if (type !in setOf(MediaType.LOGO, MediaType.POSE) ||
                contentType !in setOf("image/jpeg", "image/png", "image/webp")
            ) {
                throw BusinessException(ResultCode.INVALID_PARAMETER)
            }

            val key = MediaKey.generateFromContentType(type, contentType)
            val media = mediaRepository.save(
                Media(
                    storageKey = key,
                    ownerId = uploaderId,
                    mediaType = type,
                    contentType = contentType,
                    width = item.width,
                    height = item.height,
                    size = item.size,
                    adminManaged = true,
                ),
            )
            media to mediaStorage.generateUploadTicket(key, contentType)
        }

        return AdminMediaDto.UploadResponse(
            method = tickets.first().second.method,
            expiresAt = tickets.first().second.expiresAt,
            items = tickets.map { (media, ticket) ->
                AdminMediaDto.UploadResponse.Item(media.id!!, ticket.url, ticket.contentType)
            },
        )
    }
}
