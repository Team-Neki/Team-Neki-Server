package com.neki.media.application

import com.neki.common.annotation.UseCase
import com.neki.media.application.dto.MediaAssembler
import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaQuery
import com.neki.media.models.Media
import com.neki.media.service.MediaBinaryService
import com.neki.media.service.MediaService

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
