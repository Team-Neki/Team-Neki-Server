package com.neki.api.share.infra.client

import com.neki.api.media.application.GetImageByKeyUseCase
import com.neki.api.media.application.dto.MediaResult
import com.neki.domain.media.dto.MediaQuery
import com.neki.domain.share.client.MediaClient
import com.neki.domain.share.models.SharedImageContent
import org.springframework.stereotype.Component

/**
 * fileName       : ShareMediaClient
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : monolithic architecture media client
 * - /file/image 와 같은 스토리지 읽기 경로(GetImageByKeyUseCase)를 재사용한다.
 * - Content-Type 은 storageKey 확장자 기준으로 /file/image 와 같은 값이 나온다.
 */
@Component
class ShareMediaClient(private val getImageByKeyUseCase: GetImageByKeyUseCase) : MediaClient {

    override fun fetchImage(storageKey: String): SharedImageContent.Binary {
        val result: MediaResult.GetImageByKey =
            getImageByKeyUseCase.execute(MediaQuery.GetImageByKey(objectKey = storageKey))

        return SharedImageContent.Binary(bytes = result.binaryData, contentType = result.contentType)
    }
}
