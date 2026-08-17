package com.neki.api.media.application

import com.neki.api.media.application.dto.MediaAssembler
import com.neki.api.media.application.dto.MediaResult
import com.neki.core.annotation.UseCase
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.models.Media
import com.neki.domain.media.service.MediaService

/**
 * fileName       : GetMediaMetadataUseCase
 * author         : koo
 * date           : 2026. 1. 26. 오후 7:08
 * description    :
 */
@UseCase
class GetMediaMetadataUseCase(private val mediaService: MediaService) {

    fun execute(query: MediaQuery.GetMediaMetadata): MediaResult.GetMediaMetadata {
        val media: Media = mediaService.getActiveMedia(query)

        return MediaResult.GetMediaMetadata(MediaAssembler.toMetadata(media))
    }
}
