package com.neki.media.application

import com.neki.common.annotation.UseCase
import com.neki.media.application.dto.MediaAssembler
import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaQuery
import com.neki.media.models.Media
import com.neki.media.service.MediaService

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
