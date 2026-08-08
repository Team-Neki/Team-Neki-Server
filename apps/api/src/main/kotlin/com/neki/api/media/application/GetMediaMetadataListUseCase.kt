package com.neki.api.media.application

import com.neki.api.media.application.dto.MediaAssembler
import com.neki.api.media.application.dto.MediaResult
import com.neki.core.annotation.UseCase
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.models.Media
import com.neki.domain.media.service.MediaService

/**
 * fileName       : GetMediaMetadataListUseCase
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : media storage key 정보 조회 usecase (이미지 URL 생성용)
 */
@UseCase
class GetMediaMetadataListUseCase(private val mediaService: MediaService) {

    fun execute(query: MediaQuery.GetMediaMetadataList): MediaResult.GetMediaMetadataList {
        val medias: List<Media> = mediaService.getActiveMedias(query)

        return MediaResult.GetMediaMetadataList(MediaAssembler.toMetadatas(medias))
    }
}
