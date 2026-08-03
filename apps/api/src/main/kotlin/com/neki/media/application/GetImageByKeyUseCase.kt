package com.neki.media.application

import com.neki.common.annotation.UseCase
import com.neki.media.application.dto.MediaResult
import com.neki.media.dto.MediaQuery
import com.neki.media.models.MediaKey
import com.neki.media.service.MediaBinaryService

/**
 * fileName       : GetImageByKeyUseCase
 * author         : koo
 * date           : 2026. 1. 21.
 * description    : object key로 이미지 바이너리 조회 (캐시 우선, cache miss 시 S3 조회)
 * - cache stampede 방지 로직은 MediaBinaryService 참고
 */
@UseCase
class GetImageByKeyUseCase(private val mediaBinaryService: MediaBinaryService) {

    fun execute(query: MediaQuery.GetImageByKey): MediaResult.GetImageByKey {
        val binaryData: ByteArray = mediaBinaryService.getBinaryByKey(query)

        return MediaResult.GetImageByKey(binaryData, MediaKey.resolveContentType(query.objectKey))
    }
}
