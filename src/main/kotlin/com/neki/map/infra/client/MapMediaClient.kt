package com.neki.map.infra.client

import com.neki.map.application.port.MediaClientPort
import com.neki.media.application.dto.MediaQuery
import com.neki.media.application.dto.MediaResult
import com.neki.media.application.usecase.GetMediaStorageInfosUseCase
import com.neki.photo.application.port.dto.MediaContract
import org.springframework.stereotype.Component

/**
 * fileName       : MapMediaClient
 * author         : darren
 * date           : 2026. 1. 22
 * description    : monolithic architecture media client
 * - media service 분리 시 OpenFeign, EventPublisher/Consumer로 변경
 */
@Component
class MapMediaClient(private val getMediaStorageInfosUseCase: GetMediaStorageInfosUseCase) : MediaClientPort {
    override fun getMediaStorageInfos(mediaIds: List<Long>): List<MediaContract.StorageInfo> {
        val result: MediaResult.GetMediaStorageInfos = getMediaStorageInfosUseCase.execute(
            MediaQuery.GetMediaStorageInfos(null, mediaIds),
        )

        return result.storageInfos.map {
            MediaContract.StorageInfo(
                mediaId = it.mediaId,
                storageKey = it.storageKey,
                contentType = it.contentType,
            )
        }
    }
}
