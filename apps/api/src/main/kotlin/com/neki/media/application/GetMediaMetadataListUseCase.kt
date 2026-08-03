package com.neki.media.application

import com.neki.common.annotation.UseCase
import com.neki.media.application.dto.MediaAssembler
import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaQuery
import com.neki.media.models.Media
import com.neki.media.service.MediaService

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
