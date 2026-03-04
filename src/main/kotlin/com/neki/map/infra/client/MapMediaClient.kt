package com.neki.map.infra.client

import com.neki.map.application.port.MediaClientPort
import com.neki.media.application.command.GetMediaStorageInfosCommand
import com.neki.media.application.result.GetMediaStorageInfosResult
import com.neki.media.application.usecase.GetMediaStorageInfosUseCase
import com.neki.photo.contract.MediaStorageInfo
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
    override fun getMediaStorageInfos(mediaIds: List<Long>): List<MediaStorageInfo> {
        val result: GetMediaStorageInfosResult = getMediaStorageInfosUseCase.execute(
            GetMediaStorageInfosCommand(null, mediaIds),
        )

        return result.storageInfos.map {
            MediaStorageInfo(
                mediaId = it.mediaId,
                storageKey = it.storageKey,
                contentType = it.contentType,
            )
        }
    }
}
