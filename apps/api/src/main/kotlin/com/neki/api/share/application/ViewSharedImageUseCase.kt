package com.neki.api.share.application

import com.neki.api.share.application.dto.ShareResult
import com.neki.api.share.config.ShareProperties
import com.neki.core.annotation.UseCase
import com.neki.domain.share.client.PhotoClient
import com.neki.domain.share.dto.ShareQuery
import com.neki.domain.share.external.SharedImageDelivery
import com.neki.domain.share.models.ShareLink
import com.neki.domain.share.models.SharePhoto
import com.neki.domain.share.models.SharedImageContent
import com.neki.domain.share.service.ShareService
import java.time.Duration
import java.time.LocalDateTime

/**
 * fileName       : ViewSharedImageUseCase
 * author         : darren
 * date           : 2026. 11. 26.
 * description    : 공유 링크로 이미지 조회 (공개)
 * - 공유 시점의 소유자(link.userId)로 사진을 다시 확인해 사진 soft delete, media 비 UPLOADED 를 410 으로 거른다.
 */
@UseCase
class ViewSharedImageUseCase(
    private val shareService: ShareService,
    private val photoClient: PhotoClient,
    private val sharedImageDelivery: SharedImageDelivery,
    private val shareProperties: ShareProperties,
) {

    fun execute(query: ShareQuery.ViewSharedImage): ShareResult.ViewSharedImage {
        val now: LocalDateTime = LocalDateTime.now()

        val link: ShareLink = shareService.getViewableLink(query, now)

        val photo: SharePhoto =
            shareService.requireViewablePhoto(photoClient.findSharablePhoto(link.userId, link.photoId))

        val maxAge: Duration = minOf(link.remaining(now), shareProperties.cacheMaxAge)

        val content: SharedImageContent = sharedImageDelivery.deliver(photo, maxAge)

        return ShareResult.ViewSharedImage(content = content, maxAge = maxAge, expiresAt = link.expiresAt)
    }
}
