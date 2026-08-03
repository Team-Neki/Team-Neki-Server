package com.neki.map.infra.client

import com.neki.map.MediaClient
import com.neki.map.models.MediaMetadata
import com.neki.media.application.GetMediaMetadataListUseCase
import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaQuery
import org.springframework.stereotype.Component

/**
 * fileName       : MapMediaClient
 * author         : darren
 * date           : 2026. 1. 22
 * description    : monolithic architecture media client
 * - media service 분리 시 OpenFeign, EventPublisher/Consumer로 변경
 */
@Component
class MapMediaClient(private val getMediaMetadataListUseCase: GetMediaMetadataListUseCase) : MediaClient {
    override fun getMediaMetadata(mediaIds: List<Long>): List<MediaMetadata> {
        val result: MediaResult.GetMediaMetadataList = getMediaMetadataListUseCase.execute(
            MediaQuery.GetMediaMetadataList(null, mediaIds),
        )

        return result.medias.map {
            MediaMetadata(
                mediaId = it.mediaId,
                storageKey = it.storageKey,
                contentType = it.contentType,
                width = it.width,
                height = it.height,
            )
        }
    }
}
