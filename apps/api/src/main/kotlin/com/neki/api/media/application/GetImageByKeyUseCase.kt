package com.neki.api.media.application

import com.neki.api.media.application.dto.MediaResult
import com.neki.core.annotation.UseCase
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.media.models.MediaKey
import com.neki.domain.media.service.MediaBinaryService

/**
 * fileName       : GetImageByKeyUseCase
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : object key로 이미지 바이너리 조회 (캐시 우선, cache miss 시 스토리지 조회)
 * - cache stampede 방지 로직은 MediaBinaryService 참고
 */
@UseCase
class GetImageByKeyUseCase(private val mediaBinaryService: MediaBinaryService) {

    fun execute(query: MediaQuery.GetImageByKey): MediaResult.GetImageByKey {
        val binaryData: ByteArray = mediaBinaryService.getBinaryByKey(query)

        return MediaResult.GetImageByKey(binaryData, MediaKey.resolveContentType(query.objectKey))
    }
}
