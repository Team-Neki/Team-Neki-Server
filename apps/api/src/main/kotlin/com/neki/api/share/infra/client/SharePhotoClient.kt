package com.neki.api.share.infra.client

import com.neki.api.photo.application.GetPhotoUseCase
import com.neki.api.photo.application.dto.PhotoImageResult
import com.neki.core.code.ResultCode
import com.neki.core.exception.BusinessException
import com.neki.domain.photo.dto.PhotoImageQuery
import com.neki.domain.share.client.PhotoClient
import com.neki.domain.share.models.SharePhoto
import org.springframework.stereotype.Component

/**
 * fileName       : SharePhotoClient
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : monolithic architecture photo client
 * - GetPhotoUseCase 로 사진 생존·소유자(@SQLRestriction)와 media UPLOADED 를 함께 확인한다.
 * - photo/media 쪽 NOT_FOUND 는 share 도메인 모델의 null 로 바꾼다. 그 외 예외는 그대로 던진다.
 */
@Component
class SharePhotoClient(private val getPhotoUseCase: GetPhotoUseCase) : PhotoClient {

    override fun findSharablePhoto(userId: Long, photoId: Long): SharePhoto? {
        val result: PhotoImageResult.GetPhoto = try {
            getPhotoUseCase.execute(PhotoImageQuery.GetPhoto(userId = userId, photoId = photoId))
        } catch (e: BusinessException) {
            if (e.resultCode == ResultCode.NOT_FOUND) return null
            throw e
        }

        return SharePhoto(photoId = result.photoId, userId = userId, storageKey = result.storageKey)
    }
}
