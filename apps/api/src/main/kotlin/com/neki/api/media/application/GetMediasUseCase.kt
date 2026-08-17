package com.neki.api.media.application

import com.neki.api.media.application.dto.MediaAssembler
import com.neki.api.media.application.dto.MediaResult
import com.neki.core.annotation.UseCase
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.models.Media
import com.neki.domain.media.service.MediaBinaryService
import com.neki.domain.media.service.MediaService

/**
 * fileName       : GetMediasUseCase
 * author         : koo
 * date           : 2026. 1. 3. 오전 3:39
 * description    : media 정보 조회 usecase
 */
@UseCase
class GetMediasUseCase(private val mediaService: MediaService, private val mediaBinaryService: MediaBinaryService) {

    fun execute(query: MediaQuery.GetMedias): MediaResult.GetMedias {
        val medias: List<Media> = mediaService.getActiveMedias(query)

        val binaryByMediaId: Map<Long, ByteArray> = medias.associate { it.id!! to mediaBinaryService.getBinary(it) }

        return MediaResult.GetMedias(MediaAssembler.toBinaries(medias, binaryByMediaId))
    }
}
