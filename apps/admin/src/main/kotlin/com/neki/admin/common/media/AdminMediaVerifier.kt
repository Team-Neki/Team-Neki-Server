package com.neki.admin.common.media

import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.media.external.MediaStorage
import com.neki.domain.media.models.Media
import com.neki.domain.media.models.MediaStatus
import com.neki.domain.media.models.MediaType
import org.springframework.stereotype.Service

@Service
class AdminMediaVerifier(
    private val mediaRepository: JpaAdminMediaRepository,
    private val mediaStorage: MediaStorage,
) {
    fun verifyAndConfirm(mediaIds: List<Long>, mediaType: MediaType) {
        val medias: List<Media> = mediaRepository.findAllById(mediaIds)
        if (medias.size != mediaIds.distinct().size || medias.any {
                it.mediaType != mediaType || it.status !in setOf(MediaStatus.INITIATED, MediaStatus.UPLOADED)
            }
        ) {
            throw BusinessException(ResultCode.UPLOAD_FAILED)
        }

        medias.filterNot { it.isUploaded() }.forEach { media ->
            if (!mediaStorage.exists(media.storageKey)) throw BusinessException(ResultCode.UPLOAD_FAILED)
            media.markAsUploaded()
        }
    }
}
